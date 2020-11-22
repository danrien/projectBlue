package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;

	private static Duration duration;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final ExoPlayer mockMediaPlayer = mock(ExoPlayer.class);
		when(mockMediaPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(75L);
		when(mockMediaPlayer.getDuration()).thenReturn(101L);

		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockMediaPlayer);
		new FuturePromise<>(exoPlayerPlaybackHandler
			.promisePlayback()
			.then(p -> {
				final ProgressedPromise<Duration, PlayedFile> returnPromise = p.promisePlayedFile();

				progress = returnPromise.getProgress();

				duration = p.getDuration();

				return null;
			}, e -> null)).get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration.millis(75));
	}

	@Test
	public void thenTheFileDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.millis(101));
	}
}
