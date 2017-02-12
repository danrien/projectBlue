package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenTwoQueuesThatEventuallyDiverge;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 1/4/17.
 */

public class WhenSwitchingQueuesAndTheNextQueueIsEmpty {
	private static IPromise<PositionedPlaybackFile> nextPreparedPlaybackFilePromise;

	@BeforeClass
	public static void before() {
		final IPositionedFileQueue positionedFileQueue = mock(IPositionedFileQueue.class);
		when(positionedFileQueue.poll())
			.thenReturn(
				new PositionedFile(1, new File(1)),
				new PositionedFile(2, new File(2)),
				new PositionedFile(3, new File(3)),
				new PositionedFile(4, new File(4)),
				new PositionedFile(5, new File(5)),
				null);

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(new FakeBufferingStatefulPlaybackHandler()),
				positionedFileQueue);

		queue.promiseNextPreparedPlaybackFile(0);
		queue.promiseNextPreparedPlaybackFile(0);

		final IPositionedFileQueue newPositionedFileQueue = mock(IPositionedFileQueue.class);

		queue.updateQueue(newPositionedFileQueue);

		nextPreparedPlaybackFilePromise = queue.promiseNextPreparedPlaybackFile(0);
	}

	@Test
	public void thenTheQueueContinues() {
		assertThat(nextPreparedPlaybackFilePromise).isNull();
	}

}
