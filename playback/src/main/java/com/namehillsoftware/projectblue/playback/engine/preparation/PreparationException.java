package com.namehillsoftware.projectblue.playback.engine.preparation;

import com.namehillsoftware.projectblue.playback.file.PositionedFile;

import java.io.IOException;


public class PreparationException extends IOException {
	private final PositionedFile positionedFile;

	PreparationException(PositionedFile positionedFile, Throwable cause) {
		super(cause);
		this.positionedFile = positionedFile;
	}

	public PositionedFile getPositionedFile() {
		return positionedFile;
	}
}
