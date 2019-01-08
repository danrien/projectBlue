package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItIsDownloaded;

import com.lasthopesoftware.bluewater.client.connection.specs.FakeConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
	private static List<StoredFileJobState> states = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final FakeConnectionProvider fakeConnectionProvider = new FakeConnectionProvider();
		fakeConnectionProvider.mapResponse(p -> new FakeConnectionProvider.ResponseTuple(200, new byte[0]));

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			fakeConnectionProvider,
			storedFileAccess,
			f -> new String[0],
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor.observeStoredFileDownload(new StoredFileJob(new ServiceFile(1), storedFile))
			.blockingSubscribe(new Observer<StoredFileJobStatus>() {
				private Disposable disposable;

				@Override
					public void onSubscribe(Disposable d) {
						this.disposable = d;
					}

					@Override
					public void onNext(StoredFileJobStatus status) {
						states.add(status.storedFileJobState);
						disposable.dispose();
					}

					@Override
					public void onError(Throwable e) {

					}

					@Override
					public void onComplete() {

					}
				});
	}

	@Test
	public void thenTheFileIsMarkedAsDownloaded() {
		verify(storedFileAccess, times(1)).markStoredFileAsDownloaded(storedFile);
	}

	@Test
	public void thenTheJobStatesProgressCorrectly() {
		assertThat(states).containsExactly(StoredFileJobState.Downloading);
	}
}
