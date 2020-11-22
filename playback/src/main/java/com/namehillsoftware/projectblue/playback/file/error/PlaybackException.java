package com.lasthopesoftware.bluewater.client.playback.file.error;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;

public class PlaybackException extends Exception {
	public final PlayableFile playbackHandler;

	public PlaybackException(PlayableFile playbackHandler) {
		this(playbackHandler, null);
	}

	public PlaybackException(PlayableFile playbackHandler, Throwable cause) {
		super(cause);
		this.playbackHandler = playbackHandler;
	}
}
