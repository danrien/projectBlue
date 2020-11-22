package com.namehillsoftware.projectblue.playback.engine.bootstrap;

import com.namehillsoftware.projectblue.playback.engine.IActivePlayer;
import com.namehillsoftware.projectblue.playback.engine.preparation.PreparedPlayableFileQueue;

import java.io.IOException;

public interface IStartPlayback {
	IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, final long filePosition) throws IOException;
}
