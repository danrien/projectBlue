package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;

import java.util.List;

/**
 * Created by david on 11/1/16.
 */

public interface IBufferingPlaybackQueuesProvider {
	IBufferingPlaybackPromiseQueue getCompletableQueue(List<IFile> playlist, int startingAt);
	IBufferingPlaybackPromiseQueue getCyclicalQueue(List<IFile> playlist, int startingAt);
}
