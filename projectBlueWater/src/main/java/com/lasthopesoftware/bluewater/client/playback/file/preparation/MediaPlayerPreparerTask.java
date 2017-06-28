package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import android.media.MediaPlayer;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.MediaPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.playback.file.initialization.IPlaybackInitialization;
import com.lasthopesoftware.promises.Messenger;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.CancellationException;

final class MediaPlayerPreparerTask implements
	OneParameterAction<Messenger<IBufferingPlaybackHandler>> {

	private final ServiceFile serviceFile;
	private final int prepareAt;
	private final IFileUriProvider uriProvider;
	private final IPlaybackInitialization<MediaPlayer> playbackInitialization;

	MediaPlayerPreparerTask(ServiceFile serviceFile, int prepareAt, IFileUriProvider uriProvider, IPlaybackInitialization<MediaPlayer> playbackInitialization) {
		this.serviceFile = serviceFile;
		this.prepareAt = prepareAt;
		this.uriProvider = uriProvider;
		this.playbackInitialization = playbackInitialization;
	}

	@Override
	public void runWith(Messenger<IBufferingPlaybackHandler> messenger) {
		uriProvider
			.getFileUri(serviceFile)
			.next(new MediaPlayerPreparationTask(playbackInitialization, prepareAt, messenger))
			.error(new UriProviderErrorHandler(messenger));
	}

	private static final class MediaPlayerPreparationTask implements CarelessOneParameterFunction<Uri, Void> {
		private final IPlaybackInitialization<MediaPlayer> playbackInitialization;
		private final int prepareAt;
		private final Messenger<IBufferingPlaybackHandler> messenger;

		MediaPlayerPreparationTask(IPlaybackInitialization<MediaPlayer> playbackInitialization, int prepareAt, Messenger<IBufferingPlaybackHandler> messenger) {
			this.playbackInitialization = playbackInitialization;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
		}

		@Override
		public Void resultFrom(Uri uri) throws Exception {
			final MediaPlayer mediaPlayer;
			mediaPlayer = playbackInitialization.initializeMediaPlayer(uri);

			final MediaPlayerPreparationHandler mediaPlayerPreparationHandler =
				new MediaPlayerPreparationHandler(mediaPlayer, prepareAt, messenger);

			messenger.cancellationRequested(mediaPlayerPreparationHandler);

			if (mediaPlayerPreparationHandler.isCancelled()) {
				messenger.sendRejection(new CancellationException());
				return null;
			}

			mediaPlayer.setOnErrorListener(mediaPlayerPreparationHandler);

			mediaPlayer.setOnPreparedListener(mediaPlayerPreparationHandler);

			mediaPlayer.prepare();

			return null;
		}
	}

	private static final class UriProviderErrorHandler implements CarelessOneParameterFunction<Throwable, Void> {

		private final Messenger messenger;

		UriProviderErrorHandler(Messenger messenger) {
			this.messenger = messenger;
		}

		@Override
		public Void resultFrom(Throwable throwable) throws Exception {
			messenger.sendRejection(throwable);
			return null;
		}
	}

	private static final class MediaPlayerPreparationHandler
		implements
			MediaPlayer.OnErrorListener,
			MediaPlayer.OnPreparedListener,
			MediaPlayer.OnSeekCompleteListener,
			Runnable
	{
		private final MediaPlayer mediaPlayer;
		private final Messenger<IBufferingPlaybackHandler> messenger;
		private final int prepareAt;

		private boolean isCancelled;

		private MediaPlayerPreparationHandler(MediaPlayer mediaPlayer, int prepareAt, Messenger<IBufferingPlaybackHandler> messenger) {
			this.mediaPlayer = mediaPlayer;
			this.prepareAt = prepareAt;
			this.messenger = messenger;
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			messenger.sendRejection(new MediaPlayerException(new EmptyPlaybackHandler(0), mp, what, extra));
			return true;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			if (isCancelled) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			if (prepareAt > 0) {
				mediaPlayer.setOnSeekCompleteListener(this);
				mediaPlayer.seekTo(prepareAt);
				return;
			}

			messenger.sendResolution(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void onSeekComplete(MediaPlayer mp) {
			if (isCancelled) {
				messenger.sendRejection(new CancellationException());
				return;
			}

			messenger.sendResolution(new MediaPlayerPlaybackHandler(mp));
		}

		@Override
		public void run() {
			isCancelled = true;

			mediaPlayer.release();
		}

		public boolean isCancelled() {
			return isCancelled;
		}
	}
}
