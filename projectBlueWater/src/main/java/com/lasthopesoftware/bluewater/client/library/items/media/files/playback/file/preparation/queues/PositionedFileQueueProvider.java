package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by david on 11/1/16.
 */
public class PositionedFileQueueProvider implements IPositionedFileQueueProvider {

	@Override
	public IPositionedFileQueue getCompletableQueue(List<File> playlist, int startingAt) {
		return new CompletingPositionedFileQueue(getTruncatedList(playlist, startingAt));
	}

	@Override
	public IPositionedFileQueue getCyclicalQueue(List<File> playlist, int startingAt) {
		final List<PositionedFile> truncatedList = getTruncatedList(playlist, startingAt);

		final int endingPosition = playlist.size() - truncatedList.size();
		for (int i = 0; i < endingPosition; i++)
			truncatedList.add(new PositionedFile(i, playlist.get(i)));

		return new RepeatingPositionedFileQueue(truncatedList);
	}

	private static List<PositionedFile> getTruncatedList(List<File> playlist, int startingAt) {
		final List<PositionedFile> positionedFiles = new ArrayList<>(playlist.size());

		for (int i = startingAt; i < playlist.size(); i++)
			positionedFiles.add(new PositionedFile(i, playlist.get(i)));

		return positionedFiles;
	}
}
