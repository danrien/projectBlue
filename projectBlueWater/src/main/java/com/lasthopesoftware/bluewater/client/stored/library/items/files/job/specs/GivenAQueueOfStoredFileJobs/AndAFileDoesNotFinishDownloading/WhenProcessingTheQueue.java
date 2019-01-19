package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAQueueOfStoredFileJobs.AndAFileDoesNotFinishDownloading;

import android.os.Build;
import android.support.annotation.RequiresApi;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import io.reactivex.Observable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheQueue {

	private static final Queue<StoredFileJob> storedFileJobs = new LinkedList<>(Arrays.asList(
		new StoredFileJob(new ServiceFile(1), new StoredFile().setServiceId(1).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(2), new StoredFile().setServiceId(2).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(4), new StoredFile().setServiceId(4).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(5), new StoredFile().setServiceId(5).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(7), new StoredFile().setServiceId(7).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(114), new StoredFile().setServiceId(114).setLibraryId(1)),
		new StoredFileJob(new ServiceFile(92), new StoredFile().setServiceId(92).setLibraryId(1))));

	private static final StoredFile[] expectedDownloadingFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(2).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(5).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1),
		new StoredFile().setServiceId(92).setLibraryId(1)
	};

	private static final StoredFile[] expectedDownloadedFiles = new StoredFile[] {
		new StoredFile().setServiceId(1).setLibraryId(1),
		new StoredFile().setServiceId(2).setLibraryId(1),
		new StoredFile().setServiceId(4).setLibraryId(1),
		new StoredFile().setServiceId(7).setLibraryId(1),
		new StoredFile().setServiceId(114).setLibraryId(1),
		new StoredFile().setServiceId(92).setLibraryId(1)
	};

	private static List<StoredFileJobStatus> storedFileStatuses;

	@RequiresApi(api = Build.VERSION_CODES.N)
	@BeforeClass
	public static void before() {
		final ProcessStoredFileJobs processStoredFileJobs = mock(ProcessStoredFileJobs.class);
		when(processStoredFileJobs.observeStoredFileDownload(any()))
			.thenAnswer(a -> Observable.just(new StoredFileJobStatus(
				mock(File.class),
				a.<StoredFileJob>getArgument(0).getStoredFile(),
				StoredFileJobState.Downloaded)));
		when(processStoredFileJobs.observeStoredFileDownload(new StoredFileJob(new ServiceFile(5), new StoredFile().setLibraryId(4).setServiceId(5))))
			.thenReturn(Observable.never());
		
		final StoredFileDownloader storedFileDownloader = new StoredFileDownloader(
			job -> Observable.just(new StoredFileJobStatus(
				mock(File.class),
				job.getStoredFile(),
				StoredFileJobState.Downloaded)));

		storedFileStatuses = storedFileDownloader.process(storedFileJobs).toList().blockingGet();
	}

	@Test
	public void thenTheFilesAreBroadcastAsDownloading() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloading)
			.map(r -> r.storedFile).toList()).containsExactly(expectedDownloadingFiles);
	}

	@Test
	public void thenAllTheFilesAreBroadcastAsDownloaded() {
		assertThat(Stream.of(storedFileStatuses).filter(s -> s.storedFileJobState == StoredFileJobState.Downloaded)
			.map(r -> r.storedFile).toList()).containsOnly(expectedDownloadedFiles);
	}
}
