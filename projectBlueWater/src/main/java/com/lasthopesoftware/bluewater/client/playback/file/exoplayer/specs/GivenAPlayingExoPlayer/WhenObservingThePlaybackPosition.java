package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingExoPlayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenObservingThePlaybackPosition {

	private static FileProgress progress;

	@BeforeClass
	public static void before() {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);

		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		exoPlayerPlaybackHandler.promisePlayback();
		progress = exoPlayerPlaybackHandler
			.observeProgress(Duration.ZERO)
			.blockingFirst();
	}

	@Test
	public void thenThePlaybackPositionIsCorrect() {
		assertThat(progress.position).isEqualTo(50);
	}

	@Test
	public void thenThePlaybackDurationIsCorrect() {
		assertThat(progress.duration).isEqualTo(100);
	}
}
