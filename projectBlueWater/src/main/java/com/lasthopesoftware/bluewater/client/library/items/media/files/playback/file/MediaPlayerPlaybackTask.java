package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 10/4/16.
 */
class MediaPlayerPlaybackTask implements TwoParameterAction<IResolvedPromise<IPlaybackHandler>, IRejectedPromise> {

	private final IPlaybackHandler playbackHandler;
	private final MediaPlayer mediaPlayer;

	MediaPlayerPlaybackTask(IPlaybackHandler playbackHandler, MediaPlayer mediaPlayer) {
		this.playbackHandler = playbackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void runWith(IResolvedPromise<IPlaybackHandler> resolve, IRejectedPromise reject) {
		mediaPlayer.setOnCompletionListener(mp -> resolve.withResult(playbackHandler));
		mediaPlayer.setOnErrorListener((mp, what, extra) -> {
			final MediaPlayerException mediaPlayerException = new MediaPlayerException(mp, what, extra);
			reject.withError(mediaPlayerException);
			return true;
		});

		mediaPlayer.start();
	}
}
