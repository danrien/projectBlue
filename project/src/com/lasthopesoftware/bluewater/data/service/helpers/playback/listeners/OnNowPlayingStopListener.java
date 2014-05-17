package com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners;

import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;

public interface OnNowPlayingStopListener {
	void onNowPlayingStop(PlaylistController controller, FilePlayer filePlayer);
}
