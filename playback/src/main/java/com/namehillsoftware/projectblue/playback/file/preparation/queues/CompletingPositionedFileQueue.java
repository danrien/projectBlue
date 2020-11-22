package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues;

import com.namehillsoftware.projectblue.playback.file.PositionedFile;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

final class CompletingPositionedFileQueue implements IPositionedFileQueue {
	private final Queue<PositionedFile> playlist;

	CompletingPositionedFileQueue(List<PositionedFile> playlist) {
		this.playlist = new ArrayDeque<>(playlist);
	}

	@Override
	public PositionedFile poll() {
		return playlist.poll();
	}

	@Override
	public PositionedFile peek() {
		return playlist.peek();
	}
}
