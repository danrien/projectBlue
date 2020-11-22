package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSwitchingQueuesAndGettingTheNextFile {

	private static final List<PositionedPlayableFile> playedFiles = new ArrayList<>();
	private static final List<PositionedPlayableFile> expectedPositionedPlayableFile = Arrays.asList(
		new PositionedPlayableFile(3, mock(PlayableFile.class), new NoTransformVolumeManager(), new ServiceFile(3)),
		new PositionedPlayableFile(4, mock(PlayableFile.class), new NoTransformVolumeManager(),new ServiceFile(4)),
		new PositionedPlayableFile(5, mock(PlayableFile.class), new NoTransformVolumeManager(),new ServiceFile(6)),
		new PositionedPlayableFile(6, mock(PlayableFile.class), new NoTransformVolumeManager(),new ServiceFile(7)));

	@BeforeClass
	public static void before() {
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
				() -> 3,
				(file, preparedAt) -> new Promise<>(new FakePreparedPlayableFile<>(new FakeBufferingPlaybackHandler())),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);
		when(newPositionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(3, new ServiceFile(3)),
				new PositionedFile(4, new ServiceFile(4)),
				new PositionedFile(5, new ServiceFile(6)),
				new PositionedFile(6, new ServiceFile(7)),
				null);

		queue.updateQueue(newPositionedFileQueue);

		queue
			.promiseNextPreparedPlaybackFile(0)
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return queue.promiseNextPreparedPlaybackFile(0);
			})
			.eventually(file -> {
				playedFiles.add(file);
				return Promise.empty();
			});
	}

	@Test
	public void thenTheQueueContinuesToCompletion() {
		assertThat(playedFiles).asList().containsExactlyElementsOf(expectedPositionedPlayableFile);
	}
}
