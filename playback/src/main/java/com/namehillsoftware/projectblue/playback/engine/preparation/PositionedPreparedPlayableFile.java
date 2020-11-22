package com.namehillsoftware.projectblue.playback.engine.preparation;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;

class PositionedPreparedPlayableFile {
	final PositionedFile positionedFile;
	final PreparedPlayableFile preparedPlayableFile;

	PositionedPreparedPlayableFile(@NonNull PositionedFile positionedFile, PreparedPlayableFile preparedPlayableFile) {
		this.positionedFile = positionedFile;
		this.preparedPlayableFile = preparedPlayableFile;
	}

	boolean isEmpty() {
		return preparedPlayableFile == null;
	}

	static PositionedPreparedPlayableFile emptyHandler(PositionedFile positionedFile) {
		return new PositionedPreparedPlayableFile(positionedFile, null);
	}
}
