package com.namehillsoftware.projectblue.playback.file.volume.preparation;

import com.namehillsoftware.projectblue.playback.file.volume.ManagePlayableFileVolume;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxFileVolumeManager implements ManagePlayableFileVolume {
	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumeManager.class);

	private final ManagePlayableFileVolume playableFileVolume;
	private float unadjustedVolume = 1;
	private float maxFileVolume = 1;

	public MaxFileVolumeManager(ManagePlayableFileVolume playableFileVolume) {
		this.playableFileVolume = playableFileVolume;
	}

	@Override
	public float setVolume(float volume) {
		unadjustedVolume = volume;
		final float adjustedVolume = maxFileVolume * volume;
		logger.debug("Volume set to " + volume + ", adjusted volume set to " + adjustedVolume);
		playableFileVolume.setVolume(adjustedVolume);
		return adjustedVolume;
	}

	@Override
	public float getVolume() {
		return playableFileVolume.getVolume();
	}

	public void setMaxFileVolume(float maxFileVolume) {
		logger.debug("Max file volume set to " + maxFileVolume);
		this.maxFileVolume = maxFileVolume;
		setVolume(unadjustedVolume);
	}
}
