package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAStandardQueue.AndASecondFileThatThrowsAnExceptionOnPreparation;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenTheQueueIsStarted {

	private static final FakeBufferingPlaybackHandler expectedPlaybackHandler = new FakeBufferingPlaybackHandler();
	private static PlayableFile returnedPlaybackHandler;
	private static Throwable error;

	@BeforeClass
	public static void before() {

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, 2)
				.map(ServiceFile::new)
				.collect(Collectors.toList());

		final PlayableFilePreparationSource playbackPreparer = mock(PlayableFilePreparationSource.class);
		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(0), 0))
			.thenReturn(new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())));

		when(playbackPreparer.promisePreparedPlaybackFile(new ServiceFile(1), 0))
			.thenReturn(new Promise<>(new Exception()))
			.thenReturn(new Promise<>(new FakePreparedPlayableFile<>(expectedPlaybackHandler)));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final int startPosition = 0;

		final PreparedPlayableFileQueue queue = new PreparedPlayableFileQueue(
			() -> 2,
			playbackPreparer,
			bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));

		queue.promiseNextPreparedPlaybackFile(0)
			.eventually(p -> queue.promiseNextPreparedPlaybackFile(0))
			.then(pf -> returnedPlaybackHandler = pf.getPlayableFile())
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
