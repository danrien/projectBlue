package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAStandardQueue;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenTheQueueIsClosed {

	private static boolean isCancelled;

	@BeforeClass
	public static void before() throws IOException {
		final CompletingFileQueueProvider bufferingPlaybackQueuesProvider = new CompletingFileQueueProvider();

		final Promise<PreparedPlayableFile> cancelRecordingPromise = new Promise<>((messenger) -> messenger.cancellationRequested(() -> isCancelled = true));

		final PreparedPlayableFileQueue queue =
			new PreparedPlayableFileQueue(
				() -> 1,
				(file, preparedAt) -> cancelRecordingPromise,
				bufferingPlaybackQueuesProvider.provideQueue(Collections.singletonList(new ServiceFile(1)), 0));

		queue.promiseNextPreparedPlaybackFile(0);

		queue.close();
	}

	@Test
	public void thenThePreparedFilesAreCancelled() {
		assertThat(isCancelled).isTrue();
	}
}
