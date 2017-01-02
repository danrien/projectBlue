package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PositionedFile;

/**
 * Created by david on 11/6/16.
 */
class PositionedBufferingPlaybackHandler {
	final PositionedFile positionedFile;
	final IBufferingPlaybackHandler bufferingPlaybackHandler;

	PositionedBufferingPlaybackHandler(PositionedFile positionedFile, IBufferingPlaybackHandler bufferingPlaybackHandler) {
		this.positionedFile = positionedFile;
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
	}
}
