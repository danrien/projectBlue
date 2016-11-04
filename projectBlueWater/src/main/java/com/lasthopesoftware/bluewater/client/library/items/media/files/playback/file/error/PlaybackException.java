package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;

/**
 * Created by david on 11/4/16.
 */

public class PlaybackException extends Exception {
	public final IPlaybackHandler playbackHandler;

	public PlaybackException(IPlaybackHandler playbackHandler) {
		this.playbackHandler = playbackHandler;
	}
}
