package com.namehillsoftware.projectblue.playback.volume

import com.namehillsoftware.projectblue.playback.file.volume.ManagePlayableFileVolume

class EmptyVolumeManager : ManagePlayableFileVolume {
	private var volume: Float = 0f

	override fun getVolume(): Float = volume

	override fun setVolume(volume: Float): Float {
		this.volume = volume
		return volume
	}
}
