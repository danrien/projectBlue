package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.ExoPlayerPlaybackErrorNotifier;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class WhenAPlaybackErrorOccurs {

	private static ExoPlayerException exception;

	@BeforeClass
	public static void before() {
		final ExoPlayerPlaybackErrorNotifier exoPlayerPlaybackCompletedNotifier = new ExoPlayerPlaybackErrorNotifier(
			new ExoPlayerPlaybackHandler(mock(ExoPlayer.class)));
		exoPlayerPlaybackCompletedNotifier.playbackError((e) -> exception = e);

		exoPlayerPlaybackCompletedNotifier.onPlayerError(ExoPlaybackException.createForSource(new IOException()));
	}

	@Test
	public void thenThePlaybackErrorIsCorrect() {
		assertThat(exception.getCause()).isInstanceOf(ExoPlaybackException.class);
	}
}
