package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.VoidFunc;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import io.reactivex.ObservableEmitter;

/**
 * Created by david on 11/8/16.
 */
public final class PlaylistPlayer implements IPlaylistPlayer, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistPlayer.class);
	private final IPreparedPlaybackFileQueue preparedPlaybackFileProvider;
	private final int preparedPosition;
	private PositionedPlaybackFile positionedPlaybackFile;
	private float volume;

	private volatile boolean isStarted;
	private ObservableEmitter<PositionedPlaybackFile> emitter;

	public PlaylistPlayer(@NotNull IPreparedPlaybackFileQueue preparedPlaybackFileProvider, int preparedPosition) {
		this.preparedPlaybackFileProvider = preparedPlaybackFileProvider;
		this.preparedPosition = preparedPosition;
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		emitter = e;

		if (!isStarted) {
			isStarted = true;
			setupNextPreparedFile(preparedPosition);
		}
	}

	public void pause() {
		if (positionedPlaybackFile == null) return;

		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		if (playbackHandler.isPlaying()) playbackHandler.pause();
	}

	@Override
	public void resume() {
		if (positionedPlaybackFile != null && !positionedPlaybackFile.getPlaybackHandler().isPlaying())
			positionedPlaybackFile.getPlaybackHandler().promisePlayback();
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;

		final PositionedPlaybackFile positionedPlaybackFile = this.positionedPlaybackFile;
		if (positionedPlaybackFile != null)
			positionedPlaybackFile.getPlaybackHandler().setVolume(volume);
	}

	private void setupNextPreparedFile() {
		setupNextPreparedFile(0);
	}

	private void setupNextPreparedFile(int preparedPosition) {
		final IPromise<PositionedPlaybackFile> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		if (preparingPlaybackFile == null) {
			doCompletion();
			return;
		}

		preparingPlaybackFile
			.then(this::changePlaybackFile)
			.thenPromise(this::startFilePlayback)
			.error(VoidFunc.running(this::handlePlaybackException));
	}

	private PositionedPlaybackFile changePlaybackFile(@NotNull PositionedPlaybackFile positionedPlaybackFile) {
		this.positionedPlaybackFile = positionedPlaybackFile;

		emitter.onNext(this.positionedPlaybackFile);

		return positionedPlaybackFile;
	}

	private IPromise<IPlaybackHandler> startFilePlayback(@NotNull PositionedPlaybackFile positionedPlaybackFile) {
		final IPlaybackHandler playbackHandler = positionedPlaybackFile.getPlaybackHandler();

		playbackHandler.setVolume(volume);

		final IPromise<IPlaybackHandler> promisedPlayback = playbackHandler.promisePlayback();

		promisedPlayback
			.then(VoidFunc.running(this::closeAndStartNextFile))
			.error(VoidFunc.running(this::handlePlaybackException));

		return promisedPlayback;
	}

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile();
	}

	private void haltPlayback() {
		try {
			if (positionedPlaybackFile != null)
				positionedPlaybackFile.getPlaybackHandler().close();

			doCompletion();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
			emitter.onError(e);
		}
	}

	private void handlePlaybackException(Exception exception) {
		emitter.onError(exception);

		haltPlayback();
	}

	@Override
	public void close() throws IOException {
		haltPlayback();
	}

	private void doCompletion() {
		this.positionedPlaybackFile = null;

		emitter.onComplete();
	}
}
