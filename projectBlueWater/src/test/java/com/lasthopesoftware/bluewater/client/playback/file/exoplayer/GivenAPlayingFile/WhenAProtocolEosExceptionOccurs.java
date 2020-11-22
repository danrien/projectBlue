package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.net.ProtocolException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenAProtocolEosExceptionOccurs {

	private static ProtocolException exoPlayerException;
	private static Player.EventListener eventListener;
	private static boolean isComplete;

	@BeforeClass
	public static void before() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListener = invocation.getArgument(0);
			return null;
		}).when(mockExoPlayer).addListener(any());

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandlerPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
			.eventually(PlayingFile::promisePlayedFile)
			.then(
				p -> isComplete = true,
				e -> {
					if (e instanceof ProtocolException) {
						exoPlayerException = (ProtocolException)e;
					}

					return isComplete = false;
				})
			.then(v -> {
				countDownLatch.countDown();
				return null;
			});

		eventListener.onPlayerError(ExoPlaybackException.createForSource(new ProtocolException("unexpected end of stream")));

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenPlaybackCompletes() {
		assertThat(isComplete).isTrue();
	}

	@Test
	public void thenNoPlaybackErrorOccurs() {
		assertThat(exoPlayerException).isNull();
	}
}
