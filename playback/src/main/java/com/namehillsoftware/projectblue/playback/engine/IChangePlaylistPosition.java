package com.namehillsoftware.projectblue.playback.engine;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;

import java.io.IOException;

public interface IChangePlaylistPosition {
	Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition) throws IOException;
}
