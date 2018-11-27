package com.lasthopesoftware.bluewater.client.playback.engine.selection.specs.GivenATypicalPreferenceManagerAndBroadcaster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenPersistingTheSelectedPlaybackEngine {
	private String broadcastEngineType;
	private String persistedEngineType;

	@Before
	public void before() throws InterruptedException {
		final CountDownLatch countDownLatch = new CountDownLatch(2);
		LocalBroadcastManager.getInstance(RuntimeEnvironment.application)
			.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					broadcastEngineType =
						intent.getStringExtra(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeKey);
					countDownLatch.countDown();
				}
			}, new IntentFilter(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged));

		PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
			.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
				persistedEngineType = sharedPreferences.getString(key, null);
				countDownLatch.countDown();
			});

		new PlaybackEngineTypeSelectionPersistence(
			RuntimeEnvironment.application,
			new PlaybackEngineTypeChangedBroadcaster(RuntimeEnvironment.application))
				.selectPlaybackEngine(PlaybackEngineType.ExoPlayer);

		countDownLatch.await(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheExoPlayerSelectionIsBroadcast() {
		assertThat(broadcastEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}

	@Test
	public void thenTheExoPlayerSelectionIsPersisted() {
		assertThat(persistedEngineType).isEqualTo(PlaybackEngineType.ExoPlayer.name());
	}
}
