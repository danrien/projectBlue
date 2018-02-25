package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class PlaybackHandlerVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {

	private final MaxFileVolumeProvider maxFileVolumeProvider;

	public PlaybackHandlerVolumeControllerFactory(MaxFileVolumeProvider maxFileVolumeProvider) {
		this.maxFileVolumeProvider = maxFileVolumeProvider;
	}

	@Override
	public IVolumeManagement manageVolume(PositionedPlayableFile positionedPlayableFile, float initialHandlerVolume) {
		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier =
			new PlaybackHandlerMaxVolumeModifier(
				positionedPlayableFile.getPlayableFileVolumeManager(),
				initialHandlerVolume);

		maxFileVolumeProvider
			.getMaxFileVolume(positionedPlayableFile.getServiceFile())
			.then(perform(playbackHandlerMaxVolumeModifier::setMaxFileVolume));

		return playbackHandlerMaxVolumeModifier;
	}
}
