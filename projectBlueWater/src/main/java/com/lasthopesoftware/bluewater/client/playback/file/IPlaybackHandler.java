package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.messenger.promise.Promise;

import java.io.Closeable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();

	void setVolume(float volume);
	float getVolume();

	int getCurrentPosition();

	int getDuration();

	Promise<IPlaybackHandler> promisePlayback();
}