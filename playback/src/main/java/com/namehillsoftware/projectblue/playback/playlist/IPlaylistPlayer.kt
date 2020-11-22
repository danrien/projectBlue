package com.lasthopesoftware.bluewater.client.playback.playlist

import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile
import io.reactivex.ObservableOnSubscribe

/**
 * Created by david on 11/7/16.
 */
interface IPlaylistPlayer : ObservableOnSubscribe<PositionedPlayingFile> {
	fun pause(): Promise<*>
	fun resume(): Promise<PositionedPlayingFile?>
	fun setVolume(volume: Float)
	val isPlaying: Boolean
}
