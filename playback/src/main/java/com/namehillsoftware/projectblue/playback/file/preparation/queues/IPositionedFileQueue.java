package com.namehillsoftware.projectblue.playback.file.preparation.queues;

import com.namehillsoftware.projectblue.playback.file.PositionedFile;

/**
 * Created by david on 11/16/16.
 */

public interface IPositionedFileQueue {
	PositionedFile poll();
	PositionedFile peek();
}
