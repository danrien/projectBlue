package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAStandardQueue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenTheQueueIsConsumed {

	private static Map<ServiceFile, MockResolveAction> fileActionMap;
	private static int returnedPromiseCount;
	private static int expectedNumberOfFiles;

	@BeforeClass
	public static void before() {

		final Random random = new Random(System.currentTimeMillis());
		expectedNumberOfFiles = random.nextInt(499) + 1;

		final List<ServiceFile> serviceFiles =
			Stream
				.range(0, expectedNumberOfFiles)
				.map(i -> new ServiceFile(random.nextInt()))
				.collect(Collectors.toList());

		fileActionMap =
			Stream
				.of(serviceFiles)
				.collect(Collectors.toMap(file -> file, file -> new MockResolveAction()));

		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider
			= new CompletingFileQueueProvider();

		final PreparedPlayableFileQueue queue =
			new PreparedPlayableFileQueue(
				() -> 1,
				(file, preparedAt) -> new Promise<>(fileActionMap.get(file)),
				bufferingPlaybackQueuesProvider.provideQueue(serviceFiles, 0));

		final int expectedCycles = random.nextInt(99) + 1;

		final int expectedNumberAbsolutePromises = expectedCycles * expectedNumberOfFiles;

		for (int i = 0; i < expectedNumberAbsolutePromises; i++) {
			final Promise<PositionedPlayableFile> positionedPlaybackFilePromise =
				queue.promiseNextPreparedPlaybackFile(0);

			if (positionedPlaybackFilePromise != null)
				++returnedPromiseCount;
		}
	}

	@Test
	public void thenEachFileIsPreparedTheAppropriateAmountOfTimes() {
		assertThat(Stream.of(fileActionMap).map(entry -> entry.getValue().calls).distinct().collect(Collectors.toList())).containsExactly(1);
	}

	@Test
	public void thenTheCorrectNumberOfPromisesIsReturned() {
		assertThat(returnedPromiseCount).isEqualTo(expectedNumberOfFiles);
	}

	private static class MockResolveAction implements MessengerOperator<PreparedPlayableFile> {
		private int calls;

		@Override
		public void send(Messenger<PreparedPlayableFile> resolve) {
			++calls;
			resolve.sendResolution(mock(PreparedPlayableFile.class));
		}
	}
}
