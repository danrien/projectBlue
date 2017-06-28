package com.lasthopesoftware.bluewater.client.playback.queues.specs.GivenACyclicQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CyclicalFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by david on 11/13/16.
 */

public class WhenTheQueueIsStarted {
	private static IPreparedPlaybackFileQueue queue;
	private static int startPosition;

	@BeforeClass
	public static void before() {
		final Random random = new Random(System.currentTimeMillis());
		final int numberOfFiles = random.nextInt(500);

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, numberOfFiles)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		Map<ServiceFile, MockResolveAction> fileActionMap =
			Stream
				.of(serviceFiles)
				.collect(Collectors.toMap(file -> file, file -> spy(new MockResolveAction())));

		final CyclicalFileQueueProvider bufferingPlaybackQueuesProvider
			= new CyclicalFileQueueProvider();

		startPosition = random.nextInt(numberOfFiles);

		queue =
			new PreparedPlaybackQueue(
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, startPosition));
	}

	@Test
	public void thenTheQueueStartsAtTheCorrectPosition() {
		queue
			.promiseNextPreparedPlaybackFile(0)
			.next(VoidFunc.runCarelessly(positionedPlaybackFile -> Assert.assertEquals(startPosition, positionedPlaybackFile.getPlaylistPosition())));
	}

	private static class MockResolveAction implements OneParameterAction<Messenger<IBufferingPlaybackHandler>> {
		@Override
		public void runWith(Messenger<IBufferingPlaybackHandler> resolve) {
			resolve.sendResolution(mock(IBufferingPlaybackHandler.class));
		}
	}
}
