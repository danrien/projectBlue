package com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer;

import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.TransferringExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.exoplayer.mediasource.DataSourceFactoryProvider;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerOperator;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.lasthopesoftware.messenger.promises.response.PromisedResponse;

import java.io.IOException;
import java.util.concurrent.CancellationException;

final class ExoPlayerPreparerTask implements PromisedResponse<Uri, PreparedPlaybackFile> {

	private final DataSourceFactoryProvider dataSourceFactoryProvider;
	private final TrackSelector trackSelector;
	private final LoadControl loadControl;
	private final RenderersFactory rendersFactory;
	private final ExtractorsFactory extractorsFactory;
	private final Handler handler;
	private final long prepareAt;
	private final ServiceFile serviceFile;

	ExoPlayerPreparerTask(DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory rendersFactory, ExtractorsFactory extractorsFactory, Handler handler, ServiceFile serviceFile, long prepareAt) {
		this.dataSourceFactoryProvider = dataSourceFactoryProvider;
		this.trackSelector = trackSelector;
		this.loadControl = loadControl;
		this.rendersFactory = rendersFactory;
		this.extractorsFactory = extractorsFactory;
		this.handler = handler;
		this.serviceFile = serviceFile;
		this.prepareAt = prepareAt;
	}

	@Override
	public Promise<PreparedPlaybackFile> promiseResponse(Uri uri) throws Throwable {
		return new Promise<>(
			new ExoPlayerPreparationOperator(
				dataSourceFactoryProvider,
				trackSelector,
				loadControl,
				rendersFactory,
				extractorsFactory,
				handler,
				serviceFile,
				uri,
				prepareAt));
	}

	private static final class ExoPlayerPreparationOperator implements MessengerOperator<PreparedPlaybackFile> {

		private final DataSourceFactoryProvider dataSourceFactoryProvider;
		private final TrackSelector trackSelector;
		private final LoadControl loadControl;
		private final RenderersFactory rendersFactory;
		private final ExtractorsFactory extractorsFactory;
		private final Handler handler;
		private final Uri uri;
		private final long prepareAt;
		private final ServiceFile serviceFile;

		ExoPlayerPreparationOperator(DataSourceFactoryProvider dataSourceFactoryProvider, TrackSelector trackSelector, LoadControl loadControl, RenderersFactory rendersFactory, ExtractorsFactory extractorsFactory, Handler handler, ServiceFile serviceFile, Uri uri, long prepareAt) {
			this.dataSourceFactoryProvider = dataSourceFactoryProvider;
			this.trackSelector = trackSelector;
			this.loadControl = loadControl;
			this.rendersFactory = rendersFactory;
			this.extractorsFactory = extractorsFactory;
			this.handler = handler;
			this.serviceFile = serviceFile;
			this.uri = uri;
			this.prepareAt = prepareAt;
		}

		@Override
		public void send(Messenger<PreparedPlaybackFile> messenger) {
			final CancellationToken cancellationToken = new CancellationToken();
			messenger.cancellationRequested(cancellationToken);

			if (cancellationToken.isCancelled()) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			final SimpleExoPlayer exoPlayer = ExoPlayerFactory.newSimpleInstance(
				rendersFactory,
				trackSelector,
				loadControl);
			if (cancellationToken.isCancelled()) {
				exoPlayer.release();
				messenger.sendRejection(new CancellationException());
				return;
			}

			final TransferringExoPlayer<? super DataSource> transferringExoPlayer = new TransferringExoPlayer<>();

			final ExoPlayerPreparationHandler exoPlayerPreparationHandler =
				new ExoPlayerPreparationHandler(exoPlayer, transferringExoPlayer, prepareAt, messenger, cancellationToken);

			exoPlayer.addListener(exoPlayerPreparationHandler);

			if (cancellationToken.isCancelled()) return;

			final MediaSource mediaSource = new ExtractorMediaSource(
				uri,
				dataSourceFactoryProvider.getFactory(uri, serviceFile, transferringExoPlayer),
				extractorsFactory,
				handler,
				exoPlayerPreparationHandler);

			try {
				exoPlayer.prepare(mediaSource);
			} catch (IllegalStateException e) {
				messenger.sendRejection(e);
			}
		}
	}

	private static final class ExoPlayerPreparationHandler
	implements
		Player.EventListener,
		ExtractorMediaSource.EventListener,
		Runnable
	{
		private final SimpleExoPlayer exoPlayer;
		private final Messenger<PreparedPlaybackFile> messenger;
		private final TransferringExoPlayer<? super DataSource> transferringExoPlayer;
		private final long prepareAt;
		private final CancellationToken cancellationToken;

		private ExoPlayerPreparationHandler(SimpleExoPlayer exoPlayer, TransferringExoPlayer<? super DataSource> transferringExoPlayer, long prepareAt, Messenger<PreparedPlaybackFile> messenger, CancellationToken cancellationToken) {
			this.exoPlayer = exoPlayer;
			this.transferringExoPlayer = transferringExoPlayer;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
			this.cancellationToken = cancellationToken;
			messenger.cancellationRequested(this);
		}

		@Override
		public void run() {
			cancellationToken.run();

			exoPlayer.release();

			messenger.sendRejection(new CancellationException());
		}

		@Override
		public void onTimelineChanged(Timeline timeline, Object manifest) {
		}

		@Override
		public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

		}

		@Override
		public void onLoadingChanged(boolean isLoading) {
		}

		@Override
		public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
			if (cancellationToken.isCancelled()) return;

			if (playbackState != Player.STATE_READY) return;

			if (exoPlayer.getCurrentPosition() < prepareAt) {
				exoPlayer.seekTo(prepareAt);
				return;
			}

			exoPlayer.removeListener(this);

			messenger.sendResolution(new PreparedPlaybackFile(new ExoPlayerPlaybackHandler(exoPlayer), transferringExoPlayer));
		}

		@Override
		public void onRepeatModeChanged(int repeatMode) {

		}

		@Override
		public void onPlayerError(ExoPlaybackException error) {
			exoPlayer.release();
			messenger.sendRejection(error);
		}

		@Override
		public void onPositionDiscontinuity() {

		}

		@Override
		public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
		}

		@Override
		public void onLoadError(IOException error) {
			exoPlayer.release();
			messenger.sendRejection(error);
		}
	}
}
