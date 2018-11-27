package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation;

import android.content.Context;
import android.os.Handler;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Minutes;


public class ExoPlayerPlayableFilePreparationSourceProvider implements IPlayableFilePreparationSourceProvider {

	private static final CreateAndHold<Integer> maxBufferMs = new Lazy<>(() -> (int) Minutes.minutes(5).toStandardDuration().getMillis());
	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewTrackSelector);
	private static final CreateAndHold<LoadControl> loadControl = new Lazy<>(ExoPlayerPlayableFilePreparationSourceProvider::getNewLoadControl);

	private final Handler handler;
	private final BestMatchUriProvider bestMatchUriProvider;
	private final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider;
	private final RenderersFactory renderersFactory;

	public ExoPlayerPlayableFilePreparationSourceProvider(Context context, Handler handler, IConnectionProvider connectionProvider, BestMatchUriProvider bestMatchUriProvider, Library library, Cache cache) {
		this.handler = handler;
		this.bestMatchUriProvider = bestMatchUriProvider;

		extractorMediaSourceFactoryProvider = new ExtractorMediaSourceFactoryProvider(
			context,
			connectionProvider,
			library,
			cache);

		renderersFactory = new DefaultRenderersFactory(context);
	}

	@Override
	public int getMaxQueueSize() {
		return 1;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new ExoPlayerPlaybackPreparer(
			extractorMediaSourceFactoryProvider,
			trackSelector.getObject(),
			loadControl.getObject(),
			renderersFactory,
			handler,
			bestMatchUriProvider);
	}

	private static TrackSelector getNewTrackSelector() {
		return new DefaultTrackSelector();
	}

	private static LoadControl getNewLoadControl() {
		return new DefaultLoadControl(
			new DefaultAllocator(false, C.DEFAULT_BUFFER_SEGMENT_SIZE),
			DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
			maxBufferMs.getObject(),
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
			DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
			DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES,
			DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS);
	}
}
