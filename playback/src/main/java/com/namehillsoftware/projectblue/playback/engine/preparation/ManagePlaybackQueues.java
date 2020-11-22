package com.namehillsoftware.projectblue.playback.engine.preparation;

import com.namehillsoftware.projectblue.playback.file.preparation.queues.IPositionedFileQueue;

import java.io.Closeable;
import java.io.IOException;

public interface ManagePlaybackQueues extends Closeable {
	PreparedPlayableFileQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) throws IOException;

	boolean tryUpdateQueue(IPositionedFileQueue positionedFileQueue);
}
