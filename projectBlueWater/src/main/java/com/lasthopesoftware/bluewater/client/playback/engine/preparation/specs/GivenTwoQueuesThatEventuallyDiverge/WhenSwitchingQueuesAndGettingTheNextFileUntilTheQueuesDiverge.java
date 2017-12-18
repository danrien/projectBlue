package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSwitchingQueuesAndGettingTheNextFileUntilTheQueuesDiverge {

	private static PositionedPlaybackFile positionedPlaybackFile;
	private static PositionedPlaybackFile expectedPositionedPlaybackFile;

	@BeforeClass
	public static void before() {
		expectedPositionedPlaybackFile = new PositionedPlaybackFile(6, mock(PlayableFile.class), new ServiceFile(6));

		final IPositionedFileQueue positionedFileQueue = mock(IPositionedFileQueue.class);
		when(positionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(1, new ServiceFile(1)),
				new PositionedFile(2, new ServiceFile(2)),
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(4, new ServiceFile(4)),
				new PositionedFile(5, new ServiceFile(5)),
				null);

		final PreparedPlayableFileQueue queue =
			new PreparedPlayableFileQueue(
				() -> 1,
				(file, preparedAt) -> new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);
		when(newPositionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(6, new ServiceFile(6)),
				null);

		queue.updateQueue(newPositionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0).then(file -> positionedPlaybackFile = file);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(positionedPlaybackFile).isEqualTo(expectedPositionedPlaybackFile);
	}
}
