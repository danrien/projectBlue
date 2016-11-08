package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedPlaybackHandlerContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by david on 11/8/16.
 */
final class PlaylistPlaybackTask implements ThreeParameterAction<IResolvedPromise<Void>, IRejectedPromise, OneParameterAction<Runnable>> {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlaybackTask.class);
	private final IPreparedPlaybackFileProvider preparedPlaybackFileProvider;
	private final int preparedPosition;
	private PositionedPlaybackHandlerContainer playbackHandlerContainer;
	private float volume;

	PlaylistPlaybackTask(IPreparedPlaybackFileProvider preparedPlaybackFileProvider, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void runWith(IResolvedPromise<Void> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
		setupNextPreparedFile(preparedPosition, resolve, reject);
	}

	public void pause() {

		if (playbackHandlerContainer == null) return;

		final IPlaybackHandler playbackHandler = playbackHandlerContainer.playbackHandler;

		if (playbackHandler.isPlaying()) playbackHandler.pause();

//		if (onNowPlayingPauseListener != null)
//			onNowPlayingPauseListener.onNowPlayingPause(this, playbackHandler);
	}

	void resume() {
		if (playbackHandlerContainer != null && !playbackHandlerContainer.playbackHandler.isPlaying())
			startFilePlayback(playbackHandlerContainer);
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	private void setupNextPreparedFile(IResolvedPromise<Void> resolve, IRejectedPromise reject) {
		setupNextPreparedFile(0, resolve, reject);
	}

	private void setupNextPreparedFile(int preparedPosition, IResolvedPromise<Void> resolve, IRejectedPromise reject) {
		final IPromise<PositionedPlaybackHandlerContainer> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			resolve.withResult(null);
			return;
		}

		preparingPlaybackFile
			.then(playbackHandlerContainer -> this.startFilePlayback(playbackHandlerContainer, resolve, reject))
			.error(reject::withError);
	}

	private void startFilePlayback(@NotNull PositionedPlaybackHandlerContainer playbackHandlerContainer, IResolvedPromise<Void> resolve, IRejectedPromise reject) {

		this.playbackHandlerContainer = playbackHandlerContainer;

		final IPlaybackHandler playbackHandler = playbackHandlerContainer.playbackHandler;

		playbackHandler.setVolume(volume);
		playbackHandler
			.promisePlayback()
			.then(this::closeAndStartNextFile)
			.error(reject::withError);
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler, IResolvedPromise<Void> resolve, IRejectedPromise reject) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile(resolve, reject);
	}

//	private void onFileError(Exception exception) {
//		if (!(exception instanceof MediaPlayerException)) {
//			logger.error("There was an error preparing the file", exception);
//			return;
//		}
//
//		final MediaPlayerException mediaPlayerException = (MediaPlayerException) exception;
//
//		logger.error("JR File error - " + mediaPlayerException.what + " - " + mediaPlayerException.extra);
//
//		// We don't know what happened, release the entire queue
////		if (!MediaPlayerException.mediaErrorExtras().contains(mediaPlayerException.extra))
////			closePreparedPlaybackFileProvider();
////
////		if (onPlaylistStateControlErrorListener != null)
////			onPlaylistStateControlErrorListener.onPlaylistStateControlError(this, mediaPlayerException);
//	}
}
