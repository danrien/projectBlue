package com.lasthopesoftware.bluewater.client.playback.engine.events;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;

public interface OnPlayingFileChanged {
	void onPlayingFileChanged(PositionedPlaybackFile positionedPlaybackFile);
}