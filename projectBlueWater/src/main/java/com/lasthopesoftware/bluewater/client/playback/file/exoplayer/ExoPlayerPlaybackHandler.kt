package com.lasthopesoftware.bluewater.client.playback.file.exoplayer

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback.PromisedPlayedExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.ExoPlayerFileProgressReader
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold
import org.joda.time.Duration
import org.slf4j.LoggerFactory

class ExoPlayerPlaybackHandler(private val exoPlayer: PromisingExoPlayer) : PlayableFile, PlayingFile, Player.EventListener, Runnable {

	companion object {
		private val logger = LoggerFactory.getLogger(ExoPlayerPlaybackHandler::class.java)
	}

	private val lazyFileProgressReader: CreateAndHold<ExoPlayerFileProgressReader> = object : AbstractSynchronousLazy<ExoPlayerFileProgressReader>() {
		override fun create(): ExoPlayerFileProgressReader {
			return ExoPlayerFileProgressReader(exoPlayer)
		}
	}

	private val exoPlayerPositionSource: CreateAndHold<ProgressedPromise<Duration, PlayedFile>> = object : AbstractSynchronousLazy<ProgressedPromise<Duration, PlayedFile>>() {
		override fun create(): ProgressedPromise<Duration, PlayedFile> {
			return PromisedPlayedExoPlayer(
				exoPlayer,
				lazyFileProgressReader.getObject(),
				this@ExoPlayerPlaybackHandler)
		}
	}

	var isPlaying = false
		private set

	private var backingDuration = Duration.ZERO

	init {
		exoPlayer.addListener(this)
	}

	private fun pause(): Promise<PromisingExoPlayer> {
		isPlaying = false
		return exoPlayer.setPlayWhenReady(false)
	}

	override fun promisePause(): Promise<PlayableFile> {
		return pause().then { this }
	}

	override fun promisePlayedFile(): ProgressedPromise<Duration, PlayedFile> {
		return exoPlayerPositionSource.getObject()
	}

	override fun getProgress(): Duration {
		return lazyFileProgressReader.getObject().progress
	}

	override val duration: Promise<Duration>
		get() {
			return exoPlayer.getDuration().then { newDuration ->
				if (newDuration == backingDuration.millis) backingDuration
				else Duration.millis(newDuration).also { backingDuration = it }
			}
		}

	override fun promisePlayback(): Promise<PlayingFile> {
		isPlaying = true
		exoPlayer.setPlayWhenReady(true)
		return Promise(this)
	}

	override fun close() {
		isPlaying = false
		exoPlayer.setPlayWhenReady(false)
		exoPlayer.stop()
		exoPlayer.release()
	}

	override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
		logger.debug("Timeline changed")
	}

	override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
		logger.debug("Tracks changed")
	}

	override fun onLoadingChanged(isLoading: Boolean) {
		logger.debug("Loading changed to $isLoading")
	}

	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		logger.debug("Playback state has changed to $playbackState")
		if (playbackState != Player.STATE_ENDED) return
		isPlaying = false
		exoPlayer.removeListener(this)
	}

	override fun onRepeatModeChanged(repeatMode: Int) {
		logger.debug("Repeat mode has changed")
	}

	override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
	override fun onPlayerError(error: ExoPlaybackException) {}
	override fun onPositionDiscontinuity(reason: Int) {}
	override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
		logger.debug("Playback parameters have changed")
	}

	override fun onSeekProcessed() {}
	override fun run() {
		close()
	}
}
