package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;

public class PreparedPlayableFile {

	private final PlayableFile playbackHandler;
	private final ManagePlayableFileVolume playableFileVolumeManager;
	private final IBufferingPlaybackFile bufferingPlaybackFile;

	public PreparedPlayableFile(PlayableFile playbackHandler, ManagePlayableFileVolume playableFileVolumeManager, IBufferingPlaybackFile bufferingPlaybackFile) {
		this.playbackHandler = playbackHandler;
		this.playableFileVolumeManager = playableFileVolumeManager;
		this.bufferingPlaybackFile = bufferingPlaybackFile;
	}

	public IBufferingPlaybackFile getBufferingPlaybackFile() {
		return bufferingPlaybackFile;
	}

	public PlayableFile getPlaybackHandler() {
		return playbackHandler;
	}

	public ManagePlayableFileVolume getPlayableFileVolumeManager() {
		return playableFileVolumeManager;
	}
}
