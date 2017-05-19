package com.lasthopesoftware.bluewater.client.playback.file.volume;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class PlaybackHandlerVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {

	private final MaxFileVolumeProvider maxFileVolumeProvider;

	public PlaybackHandlerVolumeControllerFactory(MaxFileVolumeProvider maxFileVolumeProvider) {
		this.maxFileVolumeProvider = maxFileVolumeProvider;
	}

	@Override
	public IVolumeManagement manageVolume(PositionedPlaybackFile positionedPlaybackFile, float initialHandlerVolume) {
		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier =
			new PlaybackHandlerMaxVolumeModifier(positionedPlaybackFile.getPlaybackHandler(), initialHandlerVolume);

		maxFileVolumeProvider
			.getMaxFileVolume(positionedPlaybackFile.getServiceFile())
			.next(runCarelessly(playbackHandlerMaxVolumeModifier::setMaxFileVolume));

		return playbackHandlerMaxVolumeModifier;
	}
}
