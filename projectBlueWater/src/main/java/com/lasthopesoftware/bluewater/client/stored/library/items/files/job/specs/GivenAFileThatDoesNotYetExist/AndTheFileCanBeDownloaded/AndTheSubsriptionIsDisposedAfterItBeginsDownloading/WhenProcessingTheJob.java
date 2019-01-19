package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.AndTheFileCanBeDownloaded.AndTheSubsriptionIsDisposedAfterItBeginsDownloading;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.DeferredPromise;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
	private static List<StoredFileJobState> states = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final Request.Builder builder = new Request.Builder();
		builder.url("http://test-connection");

		final Buffer buffer = new Buffer();

		final Response.Builder responseBuilder = new Response.Builder();
		responseBuilder
			.request(builder.build())
			.protocol(Protocol.HTTP_1_1)
			.message("OK")
			.body(new RealResponseBody(null, 0, buffer))
			.code(200);

		final DeferredPromise<Response> deferredPromise = new DeferredPromise<>(responseBuilder.build());
		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);
		when(fakeConnectionProvider.promiseResponse(any()))
			.thenReturn(deferredPromise);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> mock(File.class),
			fakeConnectionProvider,
			storedFileAccess,
			f -> new String[0],
			f -> false,
			f -> true,
			(is, f) -> {});

		storedFileJobProcessor.observeStoredFileDownload(Collections.singleton(new StoredFileJob(new ServiceFile(1), storedFile)))
			.blockingSubscribe(new Observer<StoredFileJobStatus>() {
				private Disposable disposable;

				@Override
					public void onSubscribe(Disposable d) {
						this.disposable = d;
					}

					@Override
					public void onNext(StoredFileJobStatus status) {
						states.add(status.storedFileJobState);

						if (status.storedFileJobState != StoredFileJobState.Downloading) return;

						disposable.dispose();
						deferredPromise.resolve();
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
	public void thenTheFileIsNotMarkedAsDownloaded() {
		verify(storedFileAccess, never()).markStoredFileAsDownloaded(storedFile);
	}

	@Test
	public void thenTheJobStatesProgressCorrectly() {
		assertThat(states).containsExactly(StoredFileJobState.Queued, StoredFileJobState.Downloading);
	}
}
