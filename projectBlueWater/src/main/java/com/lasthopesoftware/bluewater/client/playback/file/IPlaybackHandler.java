package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.messenger.promises.Promise;

import java.io.Closeable;

public interface IPlaybackHandler extends Closeable {
	boolean isPlaying();
	void pause();

	void setVolume(float volume);

	float getVolume();

	long getCurrentPosition();

	long getDuration();

	Promise<IPlaybackHandler> promisePlayback();
}