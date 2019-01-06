package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.io.IFileStreamWriter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.storage.read.permissions.IFileReadPossibleArbitrator;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.runnables.OneParameterAction;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public final class StoredFileDownloader implements IStoredFileDownloader {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileDownloader.class);
	@NonNull
	private final ProcessStoredFileJobs storedFileJobs;

	private boolean isProcessing;

	private OneParameterAction<StoredFile> onFileDownloading;
	private OneParameterAction<StoredFile> onFileQueued;
	private OneParameterAction<StoredFile> onFileReadError;
	private OneParameterAction<StoredFile> onFileWriteError;

	private final CancellationToken cancellationToken = new CancellationToken();

	public StoredFileDownloader(@NonNull ProcessStoredFileJobs storedFileJobs) {
		this.storedFileJobs = storedFileJobs;
	}

	public StoredFileDownloader(@NonNull IStoredFileSystemFileProducer storedFileSystemFileProducer, @NonNull IConnectionProvider connectionProvider, @NonNull IStoredFileAccess storedFileAccess, @NonNull IServiceFileUriQueryParamsProvider serviceFileQueryUriParamsProvider, @NonNull IFileReadPossibleArbitrator fileReadPossibleArbitrator, @NonNull IFileWritePossibleArbitrator fileWritePossibleArbitrator, @NonNull IFileStreamWriter fileStreamWriter) {
		storedFileJobs = null;
	}

	@Override
	public Observable<StoredFileJobStatus> process(Queue<StoredFileJob> jobsQueue) {
		if (cancellationToken.isCancelled())
			throw new IllegalStateException("Processing cannot be started once the stored serviceFile downloader has been cancelled.");

		if (isProcessing)
			throw new IllegalStateException("Processing has already begun");

		isProcessing = true;

		return Observable.merge(Stream.of(jobsQueue).map(this::processStoredFileJob).toList())
			.filter(storedFileJobStatus -> storedFileJobStatus.storedFileJobState != StoredFileJobState.None);
	}

	private Observable<StoredFileJobStatus> processStoredFileJob(StoredFileJob storedFileJob) {
		if (onFileDownloading != null)
			onFileDownloading.runWith(storedFileJob.getStoredFile());

		return storedFileJobs
			.observeStoredFileDownload(storedFileJob)
			.onErrorReturn(e -> {
				if (e instanceof StoredFileWriteException) {
					onFileWriteError.runWith(((StoredFileWriteException) e).getStoredFile());
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StoredFileReadException) {
					onFileReadError.runWith(((StoredFileReadException) e).getStoredFile());
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StoredFileJobException) {
					logger.error("There was an error downloading the stored file " + ((StoredFileJobException) e).getStoredFile(), e);
					return StoredFileJobStatus.empty();
				}

				if (e instanceof StorageCreatePathException) {
					logger.error("There was an error creating the path", e);
					return StoredFileJobStatus.empty();
				}

				if (e instanceof Exception) throw (Exception) e;

				throw new RuntimeException(e);
			});
	}

	@Override
	public void setOnFileQueued(@Nullable OneParameterAction<StoredFile> onFileQueued) {
		this.onFileQueued = onFileQueued;
	}

	@Override
	public void setOnFileDownloading(@Nullable OneParameterAction<StoredFile> onFileDownloading) {
		this.onFileDownloading = onFileDownloading;
	}

	@Override
	public void setOnFileReadError(@Nullable OneParameterAction<StoredFile> onFileReadError) {
		this.onFileReadError = onFileReadError;
	}

	@Override
	public void setOnFileWriteError(@Nullable OneParameterAction<StoredFile> onFileWriteError) {
		this.onFileWriteError = onFileWriteError;
	}
}
