package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class WhenPlaybackCompletes {

	private static final List<Player.EventListener> eventListeners = new ArrayList<>();

	private static PlayedFile playedFile;

	@BeforeClass
	public static void context() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		doAnswer((Answer<Void>) invocation -> {
			eventListeners.add(invocation.getArgument(0));
			return null;
		}).when(mockExoPlayer).addListener(any());
		final ExoPlayerPlaybackHandler playbackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		playbackHandler
			.promisePlayback()
			.eventually(PlayingFile::promisePlayedFile)
			.then(p -> {
				playedFile = p;
				countDownLatch.countDown();
				return null;
			});

		Stream.of(eventListeners).forEach(e -> e.onPlayerStateChanged(false, Player.
			STATE_ENDED));

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenThePlayedFileIsReturned() {
		assertThat(playedFile).isNotNull();
	}
}
