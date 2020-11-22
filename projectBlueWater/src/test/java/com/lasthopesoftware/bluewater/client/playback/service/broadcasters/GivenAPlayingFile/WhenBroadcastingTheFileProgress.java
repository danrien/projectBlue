package com.lasthopesoftware.bluewater.client.playback.service.broadcasters.GivenAPlayingFile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WhenBroadcastingTheFileProgress {

	private static long progress;

	private static long duration;

	private static final Lazy<Void> setupTest = new Lazy<>(() -> {
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(RuntimeEnvironment.application);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				duration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration,-1);
				progress = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
				countDownLatch.countDown();
			}
		}, new IntentFilter(TrackPositionBroadcaster.trackPositionUpdate));

		final TrackPositionBroadcaster trackPositionBroadcaster = new TrackPositionBroadcaster(
			localBroadcastManager,
			new PlayingFile() {
				@Override
				public Promise<PlayableFile> promisePause() {
					return Promise.empty();
				}

				@Override
				public ProgressedPromise<Duration, PlayedFile> promisePlayedFile() {
					return new ProgressingPromise<Duration, PlayedFile>() {
						@Override
						public Duration getProgress() {
							return Duration.ZERO;
						}
					};
				}

				@Override
				public Duration getDuration() {
					return Duration.standardMinutes(3);
				}
			});

		trackPositionBroadcaster.accept(
			Duration
				.standardSeconds(2)
				.plus(Duration.standardSeconds(30)));

		countDownLatch.await();

		return null;
	});

	@Before
	public void before() {
		setupTest.getObject();
	}

	@Test
	public void thenTheProgressIsCorrect() {
		assertThat(progress).isEqualTo(Duration
			.standardSeconds(2)
			.plus(Duration.standardSeconds(30)).getMillis());
	}

	@Test
	public void thenTheDurationIsCorrect() {
		assertThat(duration).isEqualTo(Duration.standardMinutes(3).getMillis());
	}
}
