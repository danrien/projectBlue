package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.messenger.promises.Promise;

import java.io.IOException;

/**
 * Created by david on 3/8/17.
 */

public class FakeBufferingPlaybackHandler
implements IBufferingPlaybackFile, IPlaybackHandler {
	private boolean isPlaying;
	private float volume;
	private int currentPosition;

	@Override
	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void pause() {
		isPlaying = false;
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
	public long getCurrentPosition() {
		return this.currentPosition;
	}

	public void setCurrentPosition(int position) {
		this.currentPosition = position;
	}

	@Override
	public long getDuration() {
		return 0;
	}

	@Override
	public Promise<IPlaybackHandler> promisePlayback() {
		isPlaying = true;
		return new Promise<>((messenger) -> {});
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}
}
