package com.namehillsoftware.projectblue.playback.file.volume;

import com.namehillsoftware.projectblue.playback.file.PositionedPlayableFile;
import com.namehillsoftware.projectblue.playback.volume.IVolumeManagement;

public interface IPlaybackHandlerVolumeControllerFactory {
	IVolumeManagement manageVolume(PositionedPlayableFile positionedPlayableFile, float initialHandlerVolume);
}
