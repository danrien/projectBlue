package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.junit.Ignore;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.annotation.LooperMode.Mode.PAUSED;

@Ignore("Looper doesn't work correctly")
@LooperMode(PAUSED)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
public class WhenPreparing extends AndroidContext {

	private static PreparedPlayableFile preparedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException, TimeoutException {
		final LoadControl loadControl = mock(LoadControl.class);
		when(loadControl.getAllocator()).thenReturn(new DefaultAllocator(true, 1024));

		final ExoPlayerPlaybackPreparer preparer = new ExoPlayerPlaybackPreparer(
			ApplicationProvider.getApplicationContext(),
			uri -> new FakeMediaSource(),
			loadControl,
			() -> {
				final MediaCodecAudioRenderer audioRenderer = mock(MediaCodecAudioRenderer.class);
				when(audioRenderer.isReady()).thenReturn(true);
				return new Promise<>(new MediaCodecAudioRenderer[] { audioRenderer });
			},
			new Handler(Looper.getMainLooper()),
			(sf) -> new Promise<>(Uri.EMPTY));

		final Promise<PreparedPlayableFile> promisedPreparedFile =
			preparer.promisePreparedPlaybackFile(
				new ServiceFile(1),
				0);

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		promisedPreparedFile
			.then(
				new VoidResponse<>(p -> countDownLatch.countDown()),
				new VoidResponse<>(p -> countDownLatch.countDown()));

		final ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
		while (countDownLatch.getCount() > 0) {
			shadowLooper.idleFor(10, TimeUnit.MINUTES);
			Thread.sleep(100);
		}

		preparedFile = new FuturePromise<>(promisedPreparedFile).get();
	}

	@Test
	public void thenAnExoPlayerIsReturned() {
		assertThat(preparedFile.getPlaybackHandler()).isInstanceOf(ExoPlayerPlaybackHandler.class);
	}

	@Test
	public void thenABufferingFileIsReturned() {
		assertThat(preparedFile.getBufferingPlaybackFile()).isInstanceOf(BufferingExoPlayer.class);
	}

	private static final class FakeMediaSource extends BaseMediaSource {

		@Override
		protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {

		}

		@Override
		protected void releaseSourceInternal() {

		}

		@Override
		public void maybeThrowSourceInfoRefreshError() throws IOException {

		}

		@Override
		public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
			return new MediaPeriod() {
				@Override
				public void prepare(Callback callback, long positionUs) {
					callback.onPrepared(this);
				}

				@Override
				public void maybeThrowPrepareError() throws IOException {

				}

				@Override
				public TrackGroupArray getTrackGroups() {
					return new TrackGroupArray();
				}

				@Override
				public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
					return 0;
				}

				@Override
				public void discardBuffer(long positionUs, boolean toKeyframe) {

				}

				@Override
				public long readDiscontinuity() {
					return 0;
				}

				@Override
				public long seekToUs(long positionUs) {
					return 0;
				}

				@Override
				public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
					return 0;
				}

				@Override
				public long getBufferedPositionUs() {
					return 0;
				}

				@Override
				public long getNextLoadPositionUs() {
					return 0;
				}

				@Override
				public boolean continueLoading(long positionUs) {
					return false;
				}

				@Override
				public boolean isLoading() {
					return false;
				}

				@Override
				public void reevaluateBuffer(long positionUs) {

				}
			};
		}

		@Override
		public void releasePeriod(MediaPeriod mediaPeriod) {

		}
	}
}
