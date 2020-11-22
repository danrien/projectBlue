package com.namehillsoftware.projectblue.playback.playlist

import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.playback.engine.preparation.PreparedPlayableFileQueue
import com.namehillsoftware.projectblue.playback.file.PlayableFile
import com.namehillsoftware.projectblue.playback.file.PlayingFile
import com.namehillsoftware.projectblue.playback.file.PositionedPlayableFile
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise
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

	override fun subscribe(e: ObservableEmitter<PositionedPlayingFile>) {
		emitter = e
		if (!isStarted) {
			isStarted = true
			setupNextPreparedFile(preparedPosition)
		}
	}

	override fun pause(): Promise<PositionedPlayableFile?> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.eventually({ promisePause() }, { promisePause() })
				.also { lastStateChangePromise = it }
		}
	}

	override fun resume(): Promise<PositionedPlayingFile?> {
		synchronized(stateChangeSync) {
			return lastStateChangePromise
				.eventually({ promiseResumption() }, { promiseResumption() })
				.also { lastStateChangePromise = it }
		}
	}

	override val isPlaying: Boolean
		get() {
			return positionedPlayingFile != null
		}

	override fun setVolume(volume: Float) {
		this.volume = volume
		positionedPlayableFile?.playableFileVolumeManager?.volume = volume
	}

	private fun promisePause(): Promise<PositionedPlayableFile?> {
		return positionedPlayingFile
			?.let {
				it.playingFile
                        .promisePause()
					?.then { p ->
						PositionedPlayableFile(p, it.playableFileVolumeManager,	it.asPositionedFile()).apply {
							positionedPlayableFile = this
							positionedPlayingFile = null
						}
					}
			}
			?: Promise(IllegalStateException("There must be a playing file in order to pause"))
	}

	private fun promiseResumption(): Promise<PositionedPlayingFile?> {
		return positionedPlayableFile
			?.let {
				it.playableFile
					?.promisePlayback()
					?.then { p ->
						PositionedPlayingFile(p, it.playableFileVolumeManager, it.asPositionedFile()).apply {
							positionedPlayingFile = this
							positionedPlayableFile = null
						}
						positionedPlayingFile
					}
			}
			?: Promise(IllegalStateException("There must be a playable file in order to resume"))
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
