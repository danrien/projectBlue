package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles;
import com.lasthopesoftware.bluewater.shared.observables.ObservedPromise;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final ProcessStoredFileJobs storedFileJobsProcessor;
	private final Library library;
	private final CollectServiceFilesForSync serviceFilesToSyncCollector;
	private final IStoredFileAccess storedFileAccess;
	private final UpdateStoredFiles storedFileUpdater;

	public LibrarySyncHandler(
		Library library,
		CollectServiceFilesForSync serviceFilesToSyncCollector,
		IStoredFileAccess storedFileAccess,
		UpdateStoredFiles storedFileUpdater,
		ProcessStoredFileJobs storedFileJobsProcessor) {
		this.library = library;
		this.serviceFilesToSyncCollector = serviceFilesToSyncCollector;
		this.storedFileAccess = storedFileAccess;
		this.storedFileUpdater = storedFileUpdater;
		this.storedFileJobsProcessor = storedFileJobsProcessor;
	}

	public Observable<StoredFileJobStatus> observeLibrarySync() {

		final Promise<Collection<ServiceFile>> promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync();

		return StreamedPromise.stream(promisedServiceFilesToSync
			.eventually(allServiceFilesToSync -> {
				final HashSet<ServiceFile> serviceFilesSet = allServiceFilesToSync instanceof HashSet ? (HashSet<ServiceFile>)allServiceFilesToSync : new HashSet<>(allServiceFilesToSync);
				final Promise<Void> pruneFilesTask = storedFileAccess.pruneStoredFiles(library, serviceFilesSet);
				pruneFilesTask.excuse(new VoidResponse<>(e -> logger.warn("There was an error pruning the files", e)));

				return pruneFilesTask.then(voids -> serviceFilesSet);
			}))
			.map(serviceFile -> {
				final Promise<StoredFileJob> promiseDownloadedStoredFile = storedFileUpdater
					.promiseStoredFileUpdate(library, serviceFile)
					.then(storedFile -> {
						if (storedFile == null || storedFile.isDownloadComplete())
							return null;

						return new StoredFileJob(serviceFile, storedFile);
					});

				promiseDownloadedStoredFile
					.excuse(r -> {
						logger.warn("An error occurred creating or updating " + serviceFile, r);
						return null;
					});

				return promiseDownloadedStoredFile;
			})
			.toList()
			.toObservable()
			.flatMap(promises -> {
				final Promise<Observable<StoredFileJobStatus>> observablePromise = Promise.whenAll(promises)
					.then(storedFileJobs -> storedFileJobsProcessor.observeStoredFileDownload(new HashSet<>(storedFileJobs)));

				return ObservedPromise.observe(observablePromise).flatMap(o -> o);
			});
	}
}
