package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.specs.GivenAStandardQueue;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 3/2/17.
 */

public class WhenTheQueueIsClosed {

	private static final IPromise<IBufferingPlaybackHandler> mockPromise = spy(new Promise<>(mock(IBufferingPlaybackHandler.class)));

	@BeforeClass
	public static void before() throws IOException {
		final PositionedFileQueueProvider bufferingPlaybackQueuesProvider = new PositionedFileQueueProvider();

		final PreparedPlaybackQueue queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> mockPromise,
				bufferingPlaybackQueuesProvider.getCompletableQueue(Collections.singletonList(new File(1)), 0));

		queue.promiseNextPreparedPlaybackFile(0);

		queue.close();
	}

	@Test
	public void thenThePreparedFilesAreCancelled() {
		verify(mockPromise).cancel();
	}
}