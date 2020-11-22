package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPausedFile;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;
	private static Duration duration;

	@BeforeClass
	public static void before() {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady()).thenReturn(false);
		when(mockMediaPlayer.getDuration()).thenReturn(203L);

		final ExoPlayerPlaybackHandler exoPlayerFileProgressReader = new ExoPlayerPlaybackHandler(mockMediaPlayer);
		progress = exoPlayerFileProgressReader.getProgress();
		duration = exoPlayerFileProgressReader.getDuration();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.ZERO);
	}

	@Test
	public void thenTheFileDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.millis(203));
	}
}
