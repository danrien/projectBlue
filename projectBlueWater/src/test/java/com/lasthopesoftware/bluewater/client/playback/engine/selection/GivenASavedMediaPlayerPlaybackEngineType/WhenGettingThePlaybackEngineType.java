package com.lasthopesoftware.bluewater.client.playback.engine.selection.specs.GivenASavedMediaPlayerPlaybackEngineType;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() throws ExecutionException, InterruptedException {
		final SharedPreferences sharedPreferences = PreferenceManager
			.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext());

		sharedPreferences.edit()
			.putString(ApplicationConstants.PreferenceConstants.playbackEngine, "MediaPlayer")
			.apply();

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(
				sharedPreferences,
				() -> new Promise<>(PlaybackEngineType.ExoPlayer));

		playbackEngineType = new FuturePromise<>(selectedPlaybackEngineTypeAccess
			.promiseSelectedPlaybackEngineType()).get();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}

	@Test
	public void thenTheExoPlayerEngineIsTheSavedEngineType() {
		assertThat(PlaybackEngineType.valueOf(PreferenceManager
			.getDefaultSharedPreferences(RuntimeEnvironment.application)
			.getString(ApplicationConstants.PreferenceConstants.playbackEngine, "")))
			.isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
