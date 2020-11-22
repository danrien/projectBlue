package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;

import java.util.List;

public class CompletingFileQueueProvider implements IPositionedFileQueueProvider {

	@Override
	public IPositionedFileQueue provideQueue(List<ServiceFile> playlist, int startingAt) {
		return new CompletingPositionedFileQueue(QueueSplicers.getTruncatedList(playlist, startingAt));
	}

	@Override
	public boolean isRepeating() {
		return false;
	}
}
