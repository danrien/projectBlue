package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.namehillsoftware.handoff.promises.Promise;

public interface IBufferingPlaybackFile {
	Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile();
}
