package com.lasthopesoftware.bluewater.client.playback.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.promises.Promise;

import java.io.Closeable;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileQueue extends Closeable {
	Promise<PositionedPlaybackFile> promiseNextPreparedPlaybackFile(int preparedAt);
}
