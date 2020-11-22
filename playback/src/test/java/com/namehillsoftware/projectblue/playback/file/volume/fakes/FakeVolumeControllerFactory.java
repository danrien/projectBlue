package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement;


public class FakeVolumeControllerFactory implements IPlaybackHandlerVolumeControllerFactory {
	@Override
	public IVolumeManagement manageVolume(PositionedPlayableFile positionedPlayableFile, float initialVolume) {
		positionedPlayableFile.getPlayableFileVolumeManager().setVolume(initialVolume);
		return new FakeVolumeManager(positionedPlayableFile);
	}

	private static class FakeVolumeManager implements IVolumeManagement {

		private final PositionedPlayableFile positionedPlayableFile;

		FakeVolumeManager(PositionedPlayableFile positionedPlayableFile) {
			this.positionedPlayableFile = positionedPlayableFile;
		}

		@Override
		public float setVolume(float volume) {
			positionedPlayableFile.getPlayableFileVolumeManager().setVolume(volume);
			return positionedPlayableFile.getPlayableFileVolumeManager().getVolume();
		}
	}
}
