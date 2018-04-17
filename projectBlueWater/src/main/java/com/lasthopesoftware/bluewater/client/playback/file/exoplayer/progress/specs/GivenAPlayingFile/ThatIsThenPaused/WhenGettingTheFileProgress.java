package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.specs.GivenAPlayingFile.ThatIsThenPaused;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;

	@BeforeClass
	public static void before() {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady())
			.thenReturn(true)
			.thenReturn(false);

		when(mockMediaPlayer.getCurrentPosition())
			.thenReturn(78L)
			.thenReturn(new Random().nextLong())
			.thenReturn(new Random().nextLong());

		final ExoPlayerFileProgressReader exoPlayerFileProgressReader = new ExoPlayerFileProgressReader(mockMediaPlayer);
		exoPlayerFileProgressReader.getFileProgress();
		progress = exoPlayerFileProgressReader.getFileProgress();
	}

	@Test
	public void thenTheFileProgressIsLastValidFileProgress() {
		assertThat(progress).isEqualTo(Duration.millis(78));
	}
}
