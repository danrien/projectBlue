package com.lasthopesoftware.bluewater.client.playback.engine.selection.specs.GivenAnUnconfiguredPlaybackEngine;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() {
		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(RuntimeEnvironment.application);

		playbackEngineType = selectedPlaybackEngineTypeAccess.getSelectedPlaybackEngineType();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
