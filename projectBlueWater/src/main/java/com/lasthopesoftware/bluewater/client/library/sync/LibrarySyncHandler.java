package com.lasthopesoftware.bluewater.client.library.sync;

import android.content.Context;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.IStoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJobResult;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.read.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.TwoParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final Library library;
	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider;
	private final IServiceFilesToSyncCollector serviceFilesToSyncCollector;
	private final IStoredFileAccess storedFileAccess;
	private final IStoredFileDownloader storedFileDownloader;
	private OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted;

	private final CancellationProxy cancellationProxy = new CancellationProxy();

	public LibrarySyncHandler(Context context, IConnectionProvider connectionProvider, Library library, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this(
			library,
			new StoredItemServiceFileCollector(new StoredItemAccess(context, library), new FileProvider(new FileStringListProvider(connectionProvider))),
			new StoredFileAccess(context, library, cachedFilePropertiesProvider),
			new StoredFileDownloader(connectionProvider, new StoredFileAccess(context, library, cachedFilePropertiesProvider), new FileReadPossibleArbitrator(), new FileWritePossibleArbitrator()),
			new LibraryStorageReadPermissionsRequirementsProvider(),
			new LibraryStorageWritePermissionsRequirementsProvider());
	}

	public LibrarySyncHandler(Library library, IServiceFilesToSyncCollector serviceFilesToSyncCollector, IStoredFileAccess storedFileAccess, IStoredFileDownloader storedFileDownloader, ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider, ILibraryStorageWritePermissionsRequirementsProvider libraryStorageWritePermissionsRequirementsProvider) {
		this.library = library;
		this.serviceFilesToSyncCollector = serviceFilesToSyncCollector;
		this.storedFileAccess = storedFileAccess;
		this.storedFileDownloader = storedFileDownloader;
		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.libraryStorageWritePermissionsRequirementsProvider = libraryStorageWritePermissionsRequirementsProvider;
		storedFileDownloader.setOnQueueProcessingCompleted(this::handleQueueProcessingCompleted);
	}

	public void setOnFileDownloading(OneParameterAction<StoredFile> onFileDownloading) {
		storedFileDownloader.setOnFileDownloading(onFileDownloading);
	}

	public void setOnFileDownloaded(OneParameterAction<StoredFileJobResult> onFileDownloaded) {
		storedFileDownloader.setOnFileDownloaded(onFileDownloaded);
	}

	public void setOnFileQueued(OneParameterAction<StoredFile> onFileQueued) {
		storedFileDownloader.setOnFileQueued(onFileQueued);
	}

	public void setOnQueueProcessingCompleted(final OneParameterAction<LibrarySyncHandler> onQueueProcessingCompleted) {
		this.onQueueProcessingCompleted = onQueueProcessingCompleted;
	}

	public void setOnFileReadError(TwoParameterAction<Library, StoredFile> onFileReadError) {
		storedFileDownloader.setOnFileReadError(storedFile -> {
			if (libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
				onFileReadError.runWith(library, storedFile);
		});
	}

	public void setOnFileWriteError(TwoParameterAction<Library, StoredFile> onFileWriteError) {
		storedFileDownloader.setOnFileWriteError(storedFile -> {
			if (libraryStorageWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(library))
				onFileWriteError.runWith(library, storedFile);
		});
	}

	public void cancel() {
		cancellationProxy.run();

		storedFileDownloader.cancel();
	}

	public void startSync() {
		final Promise<Collection<ServiceFile>> promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync();
		cancellationProxy.doCancel(promisedServiceFilesToSync);

		promisedServiceFilesToSync
			.eventually(allServiceFilesToSync -> {
				final HashSet<ServiceFile> serviceFilesSet = allServiceFilesToSync instanceof HashSet ? (HashSet<ServiceFile>)allServiceFilesToSync : new HashSet<>(allServiceFilesToSync);
				final Promise<Collection<Void>> pruneFilesTask = storedFileAccess.pruneStoredFiles(serviceFilesSet);
				pruneFilesTask.excuse(perform(e -> logger.warn("There was an error pruning the files", e)));

				return !cancellationProxy.isCancelled()
					? pruneFilesTask.then(voids -> serviceFilesSet)
					: new Promise<Set<ServiceFile>>(Collections.emptySet());
			})
			.eventually(allServiceFilesToSync -> {
				if (cancellationProxy.isCancelled())
					return new Promise<>(Collections.emptySet());

				final List<Promise<StoredFile>> upsertFiles = Stream.of(allServiceFilesToSync)
					.map(serviceFile -> {
						if (cancellationProxy.isCancelled())
							return new Promise<>((StoredFile) null);

						final Promise<StoredFile> promiseDownloadedStoredFile = storedFileAccess
							.createOrUpdateFile(serviceFile)
							.then(storedFile -> {
								if (storedFile != null && !storedFile.isDownloadComplete())
									storedFileDownloader.queueFileForDownload(serviceFile, storedFile);

								return storedFile;
							});

						promiseDownloadedStoredFile
							.excuse(r -> {
								logger.warn("An error occurred creating or updating " + serviceFile, r);
								return null;
							});

						return promiseDownloadedStoredFile;
					})
					.toList();

				return Promise.whenAll(upsertFiles);
			})
			.then(vs -> {
				if (!cancellationProxy.isCancelled())
					storedFileDownloader.process();
				else
					handleQueueProcessingCompleted();

				return null;
			})
			.excuse(e -> {
				logger.warn("There was an error retrieving the files", e);

				handleQueueProcessingCompleted();

				return null;
			});
	}

	private void handleQueueProcessingCompleted() {
		if (onQueueProcessingCompleted != null)
			onQueueProcessingCompleted.runWith(this);
	}
}
