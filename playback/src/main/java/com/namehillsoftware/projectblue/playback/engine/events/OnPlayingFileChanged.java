package com.namehillsoftware.projectblue.playback.engine.events;

import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;

public interface OnPlayingFileChanged {
	void onPlayingFileChanged(PositionedPlayingFile positionedPlayingFile);
}
