package com.lasthopesoftware.bluewater.client.playback.file;

import com.namehillsoftware.handoff.promises.Promise;

import java.io.Closeable;

public interface PlayableFile extends Closeable {
	boolean isPlaying();
	
	void pause();

	Promise<PlayingFile> promisePlayback();
}