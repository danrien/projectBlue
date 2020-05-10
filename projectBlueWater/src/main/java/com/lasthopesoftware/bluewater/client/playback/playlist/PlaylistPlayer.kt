package com.lasthopesoftware.bluewater.client.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.EventualAction
import io.reactivex.ObservableEmitter
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

class PlaylistPlayer(private val preparedPlaybackFileProvider: PreparedPlayableFileQueue, private val preparedPosition: Long) : IPlaylistPlayer, Closeable {

	companion object {
		private val logger = LoggerFactory.getLogger(PlaylistPlayer::class.java)
	}

	private val stateChangeSync = Any()
	private var positionedPlayingFile: PositionedPlayingFile? = null
	private var positionedPlayableFile: PositionedPlayableFile? = null
	private var volume = 0f

	private var lastStateChangePromise: Promise<*> = Promise.empty<Any>()

	@Volatile
	private var isStarted = false
	private var emitter: ObservableEmitter<PositionedPlayingFile>? = null

	private val pausedPromise = EventualAction {
		val currentPlayingFile = positionedPlayingFile
		currentPlayingFile
			?.playingFile
			?.promisePause()
			?.then { p ->
				positionedPlayableFile = PositionedPlayableFile(
					p,
					currentPlayingFile.playableFileVolumeManager,
					currentPlayingFile.asPositionedFile())
				positionedPlayingFile = null
			}
			?: Promise.empty<Any>()
	}

	private val resumePromise = EventualAction {
		val currentPlayableFile = positionedPlayableFile
		currentPlayableFile
			?.playableFile
			?.promisePlayback()
			?.then { p ->
				positionedPlayingFile = PositionedPlayingFile(
					p,
					currentPlayableFile.playableFileVolumeManager,
					currentPlayableFile.asPositionedFile())
				positionedPlayableFile = null
			}
			?: Promise.empty<Any>()
	}

	override fun subscribe(e: ObservableEmitter<PositionedPlayingFile>) {
		emitter = e
		if (!isStarted) {
			isStarted = true
			setupNextPreparedFile(preparedPosition)
		}
	}

	override fun pause(): Promise<*> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.inevitably(pausedPromise).also { lastStateChangePromise = it }
		}
	}

	override fun resume(): Promise<*> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.inevitably(resumePromise).also { lastStateChangePromise = it }
		}
	}

	override fun isPlaying(): Boolean {
		return positionedPlayingFile != null
	}

	override fun setVolume(volume: Float) {
		this.volume = volume
		positionedPlayableFile?.playableFileVolumeManager?.volume = volume
	}

	private fun setupNextPreparedFile(preparedPosition: Long = 0) {
		val preparingPlaybackFile = preparedPlaybackFileProvider
			.promiseNextPreparedPlaybackFile(preparedPosition)

		if (preparingPlaybackFile == null) {
			doCompletion()
			return
		}

		preparingPlaybackFile
			.eventually { positionedPlayableFile -> startFilePlayback(positionedPlayableFile) }
			.excuse { exception -> handlePlaybackException(exception) }
	}

	private fun startFilePlayback(positionedPlayableFile: PositionedPlayableFile): Promise<PlayingFile?> {
		positionedPlayableFile.playableFileVolumeManager.volume = volume
		val playbackHandler = positionedPlayableFile.playableFile

		synchronized(stateChangeSync) {
			this.positionedPlayableFile = positionedPlayableFile
			val promisedPlayback = lastStateChangePromise.eventually(
				{ playbackHandler.promisePlayback() },
				{ playbackHandler.promisePlayback() })

			lastStateChangePromise = promisedPlayback
				.then { playingFile ->
					val newPositionedPlayingFile = PositionedPlayingFile(
						playingFile,
						positionedPlayableFile.playableFileVolumeManager,
						positionedPlayableFile.asPositionedFile())

					positionedPlayingFile = newPositionedPlayingFile

					emitter?.onNext(newPositionedPlayingFile)

					newPositionedPlayingFile
						.playingFile
						.promisePlayedFile()
						.then(
							{ closeAndStartNextFile(playbackHandler) },
							{ handlePlaybackException(it) })
				}
			return promisedPlayback
		}
	}

	private fun closeAndStartNextFile(playbackHandler: PlayableFile) {
		try {
			playbackHandler.close()
		} catch (e: IOException) {
			logger.error("There was an error releasing the media player", e)
		}
		setupNextPreparedFile()
	}

	private fun haltPlayback() {
		synchronized(stateChangeSync) {
			lastStateChangePromise = lastStateChangePromise
				.eventually(
					{ generateHaltPromise() },
					{ generateHaltPromise() })
				.then({ p ->
					p?.close()
					doCompletion()
				}, { e ->
					logger.error("There was an error releasing the media player", e)
					emitter?.onError(e)
				})
		}
	}

	private fun generateHaltPromise(): Promise<PlayableFile> {
		return positionedPlayableFile?.playableFile?.toPromise()
			?: positionedPlayingFile?.playingFile?.promisePause()
			?: Promise.empty()
	}

	private fun handlePlaybackException(exception: Throwable) {
		emitter?.onError(exception)
		haltPlayback()
	}

	override fun close() {
		haltPlayback()
	}

	private fun doCompletion() {
		positionedPlayingFile = null
		emitter?.onComplete()
	}
}
