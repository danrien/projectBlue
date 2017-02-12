package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 2/4/17.
 */

public class EmptyPlaybackHandler implements IBufferingPlaybackHandler {

	private final int duration;
	private float volume;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {

	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
	}

	@Override
	public float getVolume() {
		return this.volume;
	}

	@Override
	public int getCurrentPosition() {
		return 0;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public IPromise<IPlaybackHandler> promisePlayback() {
		return new Promise<>(this);
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public IPromise<IBufferingPlaybackHandler> bufferPlaybackFile() {
		return new Promise<>(this);
	}
}
