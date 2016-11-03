package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaybackController;

public interface OnNowPlayingStartListener {
	void onNowPlayingStart(PlaybackController controller, IPlaybackHandler playbackHandler);
}
