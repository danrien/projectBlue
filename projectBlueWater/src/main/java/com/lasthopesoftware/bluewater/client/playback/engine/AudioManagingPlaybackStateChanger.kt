package com.lasthopesoftware.bluewater.client.playback.engine

import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.audiomanager.promiseAudioFocus
import com.lasthopesoftware.bluewater.client.playback.volume.IVolumeManagement
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class AudioManagingPlaybackStateChanger(private val innerPlaybackState: ChangePlaybackState, private val audioManager: AudioManager, private val volumeManager: IVolumeManagement)
	: ChangePlaybackState, AutoCloseable, AudioManager.OnAudioFocusChangeListener {

	private val lazyAudioRequest = lazy {
		AudioFocusRequestCompat
			.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
			.setAudioAttributes(AudioAttributesCompat.Builder()
				.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
				.setUsage(AudioAttributesCompat.USAGE_MEDIA)
				.build())
			.setOnAudioFocusChangeListener(this)
			.build()
	}

	private var audioFocusSync = Any()
	private var audioFocusPromise: Promise<AudioFocusRequestCompat> = Promise.empty()
	private var isPlaying = false

	override fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<Unit> {
		isPlaying = true
		return getNewAudioFocusRequest()
			.eventually { innerPlaybackState.startPlaylist(playlist, playlistPosition, filePosition) }
	}

	override fun resume(): Promise<Unit> {
		isPlaying = true
		return getNewAudioFocusRequest()
			.eventually { innerPlaybackState.resume() }
	}

	override fun pause(): Promise<Unit> {
		isPlaying = false
		return innerPlaybackState
				.pause()
				.eventually { abandonAudioFocus() }
				.unitResponse()
	}

	override fun close() {
		abandonAudioFocus()
	}

	override fun onAudioFocusChange(focusChange: Int) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			volumeManager.setVolume(1.0f)
			if (!isPlaying) resume()
			return
		}

		if (!isPlaying) return

		when (focusChange) {
			AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
				// Lost focus but it will be regained... cannot release resources
				isPlaying = false
				innerPlaybackState.pause()
			}
			AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
				// Lost focus for a short time, but it's ok to keep playing at an attenuated level
				volumeManager.setVolume(0.2f)
		}
	}

	private fun getNewAudioFocusRequest(): Promise<AudioFocusRequestCompat> =
		synchronized(audioFocusSync) {
			abandonAudioFocus()
				.eventually(
					{ audioManager.promiseAudioFocus(lazyAudioRequest.value) },
					{ audioManager.promiseAudioFocus(lazyAudioRequest.value) })
				.also { audioFocusPromise = it }
		}

	private fun abandonAudioFocus() =
		synchronized(audioFocusSync) {
			audioFocusPromise.cancel()
			audioFocusPromise.then {
				it?.apply { AudioManagerCompat.abandonAudioFocusRequest(audioManager, this) }
			}
		}
}
