package com.lasthopesoftware.bluewater.client.playback.engine.selection.GivenAnUnconfiguredPlaybackEngine;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.shared.promises.extensions.specs.FuturePromise;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() throws ExecutionException, InterruptedException {
		final SharedPreferences sharedPreferences = PreferenceManager
			.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(
				sharedPreferences,
				() -> new Promise<>(PlaybackEngineType.ExoPlayer));

		playbackEngineType = new FuturePromise<>(selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()).get();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
