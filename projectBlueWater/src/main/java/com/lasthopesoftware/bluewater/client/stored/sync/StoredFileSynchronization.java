package com.lasthopesoftware.bluewater.client.stored.sync;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.observables.StreamedPromise;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;

public class StoredFileSynchronization implements SynchronizeStoredFiles {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(StoredFileSynchronization.class);

	public static final String onSyncStartEvent = magicPropertyBuilder.buildProperty("onSyncStartEvent");
	public static final String onSyncStopEvent = magicPropertyBuilder.buildProperty("onSyncStopEvent");
	public static final String onFileQueuedEvent = magicPropertyBuilder.buildProperty("onFileQueuedEvent");
	public static final String onFileDownloadingEvent = magicPropertyBuilder.buildProperty("onFileDownloadingEvent");
	public static final String onFileDownloadedEvent = magicPropertyBuilder.buildProperty("onFileDownloadedEvent");
	public static final String onFileWriteErrorEvent = magicPropertyBuilder.buildProperty("onFileWriteErrorEvent");
	public static final String onFileReadErrorEvent = magicPropertyBuilder.buildProperty("onFileReadErrorEvent");
	public static final String storedFileEventKey = magicPropertyBuilder.buildProperty("storedFileEventKey");

	private static final Logger logger = LoggerFactory.getLogger(StoredFileSynchronization.class);
	private final ILibraryProvider libraryProvider;
	private final LocalBroadcastManager localBroadcastManager;
	private LibrarySyncHandler syncHandler;

	public StoredFileSynchronization(
		ILibraryProvider libraryProvider,
		LocalBroadcastManager localBroadcastManager,
		LibrarySyncHandler syncHandler) {
		this.libraryProvider = libraryProvider;
		this.localBroadcastManager = localBroadcastManager;
		this.syncHandler = syncHandler;
	}

	@Override
	public Completable streamFileSynchronization() {
		logger.info("Starting sync.");

		localBroadcastManager.sendBroadcast(new Intent(onSyncStartEvent));

		return StreamedPromise.stream(libraryProvider.getAllLibraries())
			.flatMap(library -> syncHandler.observeLibrarySync(library.getLibraryId()), true)
			.flatMapCompletable(storedFileJobStatus -> {
				switch (storedFileJobStatus.storedFileJobState) {
					case Queued:
						sendStoredFileBroadcast(onFileQueuedEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
					case Downloading:
						sendStoredFileBroadcast(onFileDownloadingEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
					case Downloaded:
						sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobStatus.storedFile);
						return Completable.complete();
				}

				return Completable.complete();
			}, true)
			.onErrorComplete(this::handleError)
			.doOnComplete(this::sendStoppedSync)
			.doOnDispose(this::sendStoppedSync);
	}

	private boolean handleError(Throwable e) {
		if (e instanceof CompositeException) {
			final CompositeException compositeException = (CompositeException)e;
			return Stream.of(compositeException.getExceptions()).allMatch(this::handleError);
		}

		if (e instanceof StoredFileWriteException) {
			sendStoredFileBroadcast(onFileWriteErrorEvent, ((StoredFileWriteException)e).getStoredFile());
			return true;
		}

		if (e instanceof StoredFileReadException) {
			sendStoredFileBroadcast(onFileReadErrorEvent, ((StoredFileReadException)e).getStoredFile());
			return true;
		}

		return e instanceof StorageCreatePathException || e instanceof StoredFileJobException;
	}

	private void sendStoppedSync() {
		localBroadcastManager.sendBroadcast(new Intent(onSyncStopEvent));
	}

	private void sendStoredFileBroadcast(@NonNull String action, @NonNull StoredFile storedFile) {
		final Intent storedFileBroadcastIntent = new Intent(action);
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.getId());
		localBroadcastManager.sendBroadcast(storedFileBroadcastIntent);
	}
}
