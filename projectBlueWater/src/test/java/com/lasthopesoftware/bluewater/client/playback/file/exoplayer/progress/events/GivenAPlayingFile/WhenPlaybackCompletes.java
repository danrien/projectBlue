package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.GivenAPlayingFile;

import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.ExoPlayerPlaybackCompletedNotifier;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenPlaybackCompletes {

	private static boolean isCompleted;

	@BeforeClass
	public static void before() {
		final ExoPlayerPlaybackCompletedNotifier exoPlayerPlaybackCompletedNotifier = new ExoPlayerPlaybackCompletedNotifier();
		exoPlayerPlaybackCompletedNotifier.playbackCompleted(() -> isCompleted = true);

		exoPlayerPlaybackCompletedNotifier.onPlayerStateChanged(false, Player.STATE_ENDED);
	}

	@Test
	public void thenPlaybackCompletes() {
		assertThat(isCompleted).isTrue();
	}
}
