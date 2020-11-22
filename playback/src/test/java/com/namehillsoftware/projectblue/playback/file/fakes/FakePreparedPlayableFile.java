package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;


public class FakePreparedPlayableFile<PlaybackHandler extends PlayableFile & IBufferingPlaybackFile> extends PreparedPlayableFile {

	public FakePreparedPlayableFile(PlaybackHandler playbackHandler) {
		super(playbackHandler, new NoTransformVolumeManager(), playbackHandler);
	}
}
