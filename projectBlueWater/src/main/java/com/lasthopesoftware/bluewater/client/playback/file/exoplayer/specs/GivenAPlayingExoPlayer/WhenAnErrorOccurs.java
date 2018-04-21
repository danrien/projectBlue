package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingExoPlayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenAnErrorOccurs {

	private static ExoPlayerException exoPlayerException;
	private static Player.EventListener eventListener;

	@BeforeClass
	public static void context() throws InterruptedException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListener = invocation.getArgument(0);
			return null;
		}).when(mockExoPlayer).addListener(any());

		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandlerPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback();
		final Observable<Duration> firstObservable =
			exoPlayerPlaybackHandlerPlayerPlaybackHandler.observeProgress(Duration.millis(500));

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		firstObservable
			.subscribe(
				p -> {},
				e -> {
					if (e instanceof ExoPlayerException)
						exoPlayerException = (ExoPlayerException)e;
				});

		eventListener.onPlayerError(ExoPlaybackException.createForSource(new IOException()));

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenThePlaybackErrorIsCorrect() {
		assertThat(exoPlayerException.getCause()).isInstanceOf(ExoPlaybackException.class);
	}
}
