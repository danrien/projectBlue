package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;


public class PlaybackHandlerMaxVolumeModifier implements IVolumeManagement {

	private final IPlaybackHandler playbackHandler;
	private float unadjustedVolume;
	private float maxFileVolume = 1;

	public PlaybackHandlerMaxVolumeModifier(IPlaybackHandler playbackHandler, float initialHandlerVolume) {
		this.playbackHandler = playbackHandler;
		this.unadjustedVolume = initialHandlerVolume;

		playbackHandler.setVolume(initialHandlerVolume);
	}

	public void setMaxFileVolume(float maxFileVolume) {
		this.maxFileVolume = maxFileVolume;
		setVolume(unadjustedVolume);
	}

	@Override
	public float setVolume(float volume) {
		unadjustedVolume = volume;
		final float adjustedVolume = maxFileVolume * volume;
		playbackHandler.setVolume(adjustedVolume);
		return adjustedVolume;
	}
}