package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAStandardQueue.AndASecondFileThatThrowsAnExceptionOnPreparation;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenTheQueueIsStarted {

	private static final FakeBufferingPlaybackHandler expectedPlaybackHandler = new FakeBufferingPlaybackHandler();
	private static IPlaybackHandler returnedPlaybackHandler;
	private static Throwable error;

	@BeforeClass
	public static void before() {

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(ServiceFile::new)
				.collect(Collectors.toList());

		final IPlaybackPreparer playbackPreparer = mock(IPlaybackPreparer.class);
		when(playbackPreparer.promisePreparedPlaybackHandler(new ServiceFile(0), 0))
			.thenReturn(new Promise<>(new FakePreparedPlaybackFile<>(new FakeBufferingPlaybackHandler())));

		when(playbackPreparer.promisePreparedPlaybackHandler(new ServiceFile(1), 0))
			.thenReturn(new Promise<>(new Exception()))
			.thenReturn(new Promise<>(new FakePreparedPlaybackFile<>(expectedPlaybackHandler)));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlaybackQueue queue = new PreparedPlaybackQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(0)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(0))
			.then(pf -> returnedPlaybackHandler = pf.getPlaybackHandler())
			.excuse(e -> error = e);
	}

	@Test
	public void thenTheExpectedPlaybackHandlerIsNotReturned() {
		assertThat(returnedPlaybackHandler).isNotEqualTo(expectedPlaybackHandler).isNull();
	}

	@Test
	public void thenTheErrorIsCaught() {
		assertThat(error).isNotNull();
	}
}
