package com.lasthopesoftware.bluewater.client.playback.engine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlayingFileChanged
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaylistReset
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.ManagePlaybackQueues
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.playback.file.PositionedFile
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile
import com.vedsoft.futures.runnables.OneParameterAction
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import org.jetbrains.annotations.Contract
import org.slf4j.LoggerFactory
import kotlin.math.max

class PlaybackEngine(
	managePlaybackQueues: ManagePlaybackQueues,
	positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>,
	private val nowPlayingRepository: INowPlayingRepository,
	private val playbackBootstrapper: IStartPlayback,
	nowPlaying: NowPlaying) :
		IChangePlaylistPosition,
		IPlaybackEngineBroadcaster,
		AutoCloseable {
	private val preparedPlaybackQueueResourceManagement = managePlaybackQueues
	private val positionedFileQueueProviders = positionedFileQueueProviders.associateBy({ it.isRepeating }, { it })

	var isPlaying = false
		private set

	private var playlist: MutableList<ServiceFile> = nowPlaying.playlist
	private var isRepeating: Boolean = nowPlaying.isRepeating
	private var playlistPosition: Int = nowPlaying.playlistPosition
	private var fileProgress: FileProgress = StaticProgressedFile(nowPlaying.filePosition)

	private var playbackSubscription: Disposable? = null
	private var activePlayer: IActivePlayer? = null
	private var onPlayingFileChanged: OnPlayingFileChanged? = null
	private var onPlaylistError: OneParameterAction<Throwable>? = null
	private var onPlaybackStarted: OnPlaybackStarted? = null
	private var onPlaybackCompleted: OnPlaybackCompleted? = null
	private var onPlaylistReset: OnPlaylistReset? = null

	fun startPlaylist(playlist: MutableList<ServiceFile>, playlistPosition: Int, filePosition: Int): Promise<*> {
		logger.info("Starting playback")
		this.playlist = playlist
		this.playlistPosition = playlistPosition
		this.fileProgress = StaticProgressedFile(filePosition.toLong())
		return saveState().then { resumePlayback() }
	}

	fun skipToNext(): Promise<PositionedFile> {
		return changePosition(
			getNextPosition(playlistPosition, playlist),
			0)
	}

	fun skipToPrevious(): Promise<PositionedFile> {
		return changePosition(getPreviousPosition(playlistPosition), 0)
	}

	@Synchronized
	override fun changePosition(playlistPosition: Int, filePosition: Int): Promise<PositionedFile> {
		playbackSubscription?.dispose()
		activePlayer = null

		this.playlistPosition = playlistPosition
		fileProgress = StaticProgressedFile(filePosition.toLong())

		return with(saveState()) {
			if (!isPlaying) then {
				val serviceFile = playlist[playlistPosition]
				PositionedFile(playlistPosition, serviceFile)
			} else eventually {
				object : Promise<PositionedFile>() {
					init {
						val queueProvider = positionedFileQueueProviders.getValue(isRepeating)
						try {
							val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement
								.initializePreparedPlaybackQueue(queueProvider.provideQueue(playlist, playlistPosition))
							startPlayback(preparedPlaybackQueue, filePosition.toLong())
								.firstElement()
								.subscribe({ resolve(it.asPositionedFile()) }, { reject(it) })
						} catch (e: Exception) {
							reject(e)
						}
					}
				}
			}
		}
	}

	fun playRepeatedly() {
		isRepeating = true
		saveState()
		updatePreparedFileQueueUsingState()
	}

	fun playToCompletion() {
		isRepeating = false
		saveState()
		updatePreparedFileQueueUsingState()
	}

	fun resume(): Promise<*> {
		return activePlayer
			?.also {
				isPlaying = true
				onPlaybackStarted?.onPlaybackStarted()
			}
			?.resume().then { onPlayingFileChanged.onPlayingFileChanged(it) }
			?: return resumePlayback().toPromise()
	}

	fun pause(): Promise<*> {
		val promisedPause = activePlayer.pause() ?: Promise.empty<Any?>()

		isPlaying = false

		return promisedPause.eventually { saveState() }
	}

	override fun setOnPlayingFileChanged(onPlayingFileChanged: OnPlayingFileChanged): PlaybackEngine {
		this.onPlayingFileChanged = onPlayingFileChanged
		return this
	}

	override fun setOnPlaylistError(onPlaylistError: OneParameterAction<Throwable>): PlaybackEngine {
		this.onPlaylistError = onPlaylistError
		return this
	}

	override fun setOnPlaybackStarted(onPlaybackStarted: OnPlaybackStarted): PlaybackEngine {
		this.onPlaybackStarted = onPlaybackStarted
		return this
	}

	override fun setOnPlaybackCompleted(onPlaybackCompleted: OnPlaybackCompleted): PlaybackEngine {
		this.onPlaybackCompleted = onPlaybackCompleted
		return this
	}

	override fun setOnPlaylistReset(onPlaylistReset: OnPlaylistReset): PlaybackEngine {
		this.onPlaylistReset = onPlaylistReset
		return this
	}

	fun addFile(serviceFile: ServiceFile): Promise<NowPlaying> {
		playlist.add(serviceFile)
		updatePreparedFileQueueUsingState()
		return saveState()
	}

	fun removeFileAtPosition(position: Int): Promise<NowPlaying> {
		val promisedSkip = if (playlistPosition == position) {
			skipToNext()
		} else {
			Promise.empty()
		}

		return promisedSkip
			.eventually {
				playlist.removeAt(position)

				if (playlistPosition > position)
					playlistPosition -= 1

				updatePreparedFileQueueUsingState()
				saveState()
			}
	}

	private fun resumePlayback() {
		val positionedFileQueueProvider = positionedFileQueueProviders.getValue(isRepeating)
		val fileQueue = positionedFileQueueProvider.provideQueue(playlist, playlistPosition)
		val preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue)
		startPlayback(preparedPlaybackQueue, fileProgress.fileProgress)
	}

	private fun startPlayback(preparedPlaybackQueue: PreparedPlayableFileQueue, filePosition: Long): ConnectableObservable<PositionedPlayingFile> {
		playbackSubscription?.dispose()

		val newPlayer = playbackBootstrapper.startPlayback(preparedPlaybackQueue, filePosition)
		activePlayer = newPlayer

		isPlaying = true
		onPlaybackStarted.onPlaybackStarted()

		val observable = newPlayer.observe()

		playbackSubscription = observable.subscribe(
			{ p ->
				isPlaying = true
				playlistPosition = p.playlistPosition
				fileProgress = ProgressingFile(p)
				saveState().then { onPlayingFileChanged.onPlayingFileChanged(p) }
			},
			{ e ->
				if (e is PreparationException) {
					playlistPosition = e.positionedFile.playlistPosition
					fileProgress = StaticProgressedFile(0)
					saveState()
				}
				onPlaylistError?.runWith(e)
			},
			{
				isPlaying = false
				activePlayer = null
				changePosition(0, 0)
					.then { positionedFile ->
						onPlaylistReset.onPlaylistReset(positionedFile)
						onPlaybackCompleted.onPlaybackCompleted()
					}
			})
		return observable
	}

	private fun updatePreparedFileQueueUsingState() {
		preparedPlaybackQueueResourceManagement.tryUpdateQueue(
			positionedFileQueueProviders.getValue(isRepeating).provideQueue(
				playlist,
				playlistPosition + 1))
	}

	private fun saveState(): Promise<NowPlaying> {
		return nowPlayingRepository
			.nowPlaying
			.eventually { np ->
				np.playlist = playlist
				np.playlistPosition = playlistPosition
				np.filePosition = fileProgress.fileProgress
				np.isRepeating = isRepeating
				nowPlayingRepository.updateNowPlaying(np)
			}
	}

	override fun close() {
		isPlaying = false
		onPlaybackStarted = null
		onPlayingFileChanged = null
		onPlaylistError = null
		activePlayer = null
		playbackSubscription?.dispose()
	}

	companion object {
		private val logger = LoggerFactory.getLogger(PlaybackEngine::class.java)

		@Contract(pure = true)
		private fun getNextPosition(startingPosition: Int, playlist: Collection<ServiceFile>): Int {
			return if (startingPosition < playlist.size - 1) startingPosition + 1 else 0
		}

		@Contract(pure = true)
		private fun getPreviousPosition(startingPosition: Int): Int {
			return max(startingPosition - 1, 0)
		}

		@JvmStatic
		fun createEngine(managePlaybackQueues: ManagePlaybackQueues, positionedFileQueueProviders: Iterable<IPositionedFileQueueProvider>, nowPlayingRepository: INowPlayingRepository, playbackBootstrapper: IStartPlayback): Promise<PlaybackEngine> {
			return nowPlayingRepository
				.nowPlaying
				.then { np ->
					PlaybackEngine(
						managePlaybackQueues,
						positionedFileQueueProviders,
						nowPlayingRepository,
						playbackBootstrapper,
						np)
				}
		}
	}

	private interface FileProgress {
		val fileProgress: Long
	}

	private class StaticProgressedFile(override val fileProgress: Long) : FileProgress

	private class ProgressingFile(val positionedPlayingFile: PositionedPlayingFile)
		: FileProgress {

		override val fileProgress: Long
			get() = positionedPlayingFile.playingFile.promisePlayedFile().progress?.millis ?: 0
	}
}
