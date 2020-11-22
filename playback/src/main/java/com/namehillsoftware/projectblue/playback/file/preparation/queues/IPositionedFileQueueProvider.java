package com.lasthopesoftware.bluewater.client.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */

public interface IPositionedFileQueueProvider {
	IPositionedFileQueue provideQueue(List<ServiceFile> playlist, int startingAt);

	boolean isRepeating();
}
