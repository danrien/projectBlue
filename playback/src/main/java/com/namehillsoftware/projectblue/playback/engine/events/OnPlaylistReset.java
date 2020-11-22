package com.namehillsoftware.projectblue.playback.engine.events;

import com.namehillsoftware.projectblue.playback.file.PositionedFile;

public interface OnPlaylistReset {
	void onPlaylistReset(PositionedFile positionedFile);
}
