package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.MediaPlayerBufferedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 9/20/16.
 */

public class MediaPlayerPlaybackHandler implements IBufferingPlaybackHandler {

	private final MediaPlayer mediaPlayer;
	private final IPromise<IBufferingPlaybackHandler> bufferingPromise;

	public MediaPlayerPlaybackHandler(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		bufferingPromise = new Promise<>(new MediaPlayerBufferedPromise(this, mediaPlayer));
	}

	@Override
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	@Override
	public void pause() {
		mediaPlayer.pause();
	}

	@Override
	public void seekTo(int pos) {
		mediaPlayer.seekTo(pos);
	}

	@Override
	public int getCurrentPosition() {
		return mediaPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return mediaPlayer.getDuration();
	}

	@Override
	public IPromise<IPlaybackHandler> start() {
		return new Promise<>(new MediaPlayerPlaybackTask(this, mediaPlayer));
	}

	@Override
	public void close() throws IOException {
		mediaPlayer.release();
	}

	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return bufferingPromise;
	}

}