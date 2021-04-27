package com.lasthopesoftware.bluewater.client.playback.engine.preparation

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.IPositionedFileQueue
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.Duration
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

class PreparedPlayableFileQueue(private val configuration: IPreparedPlaybackQueueConfiguration, private val playbackPreparer: PlayableFilePreparationSource, private var positionedFileQueue: IPositionedFileQueue) : Closeable {

	companion object {
		private val logger = LoggerFactory.getLogger(PreparedPlayableFileQueue::class.java)

		private fun <T> Queue<T>.pollUntilEmpty(): Iterable<T> {
			val queue = this
			return Iterable {
				iterator {
					val value = queue.poll()
					if (value != null)
						yield(value)
				}
			}
		}
	}

	private val queueUpdateLock = ReentrantReadWriteLock()
	private val bufferingMediaPlayerPromises = ConcurrentLinkedQueue<ProvidePreparedPlaybackFile>()
	private var currentPreparingPlaybackHandlerPromise: ProvidePreparedPlaybackFile? = null

	fun updateQueue(newPositionedFileQueue: IPositionedFileQueue): PreparedPlayableFileQueue {
		val writeLock: Lock = queueUpdateLock.writeLock()
		writeLock.lock()
		return try {
			val newPositionedPreparingMediaPlayerPromises = LinkedList<ProvidePreparedPlaybackFile>()

			for (positionedPreparingFile in bufferingMediaPlayerPromises.pollUntilEmpty()) {
				if (positionedPreparingFile.positionedFile == newPositionedFileQueue.peek()) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile)
					newPositionedFileQueue.poll()
					continue
				}

				for (file in bufferingMediaPlayerPromises.pollUntilEmpty()) {
					file.preparedPlaybackFilePromise.cancel()
				}
			}

			for (positionedPreparingFile in newPositionedPreparingMediaPlayerPromises.pollUntilEmpty()) {
				bufferingMediaPlayerPromises.offer(positionedPreparingFile)
			}

			positionedFileQueue = newPositionedFileQueue
			beginQueueingPreparingPlayers()
			this
		} finally {
			writeLock.unlock()
		}
	}

	fun promiseNextPreparedPlaybackFile(preparedAt: Duration): Promise<PositionedPlayableFile>? {
		return bufferingMediaPlayerPromises.poll()?.let {
			currentPreparingPlaybackHandlerPromise = it
			Promise.whenAny(
				it.promisePositionedPreparedPlaybackFile(),
				Promise(PositionedPreparedPlayableFile.emptyHandler(it.positionedFile)))
				.eventually(::preparePlayableFileAgainIfNecessary)
				.then(::toPositionedPlayableFile)
		} ?: getNextPreparingMediaPlayerPromise(preparedAt).let {
			currentPreparingPlaybackHandlerPromise = it
			it?.promisePositionedPreparedPlaybackFile()?.then(::toPositionedPlayableFile)
		}
	}

	override fun close() {
		currentPreparingPlaybackHandlerPromise?.preparedPlaybackFilePromise?.cancel()

		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		try {
			for (positionedPreparingFile in bufferingMediaPlayerPromises.pollUntilEmpty())
				positionedPreparingFile.preparedPlaybackFilePromise.cancel()
		} finally {
			writeLock.unlock()
		}
	}

	private fun getNextPreparingMediaPlayerPromise(preparedAt: Duration): PositionedUnerringPreparingFile? {
		val writeLock = queueUpdateLock.writeLock()
		writeLock.lock()
		return try {
			positionedFileQueue.poll()
		} finally {
			writeLock.unlock()
		}?.let {
			PositionedUnerringPreparingFile(
				it,
				playbackPreparer.promisePreparedPlaybackFile(it.serviceFile, preparedAt))
		}
	}

	private fun beginQueueingPreparingPlayers() {
		val writeLock: Lock = queueUpdateLock.writeLock()
		writeLock.lock()
		try {
			if (bufferingMediaPlayerPromises.size >= configuration.maxQueueSize) return
			val nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(Duration.ZERO)
				?: return
			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise)
			nextPreparingMediaPlayerPromise.promisePositionedPreparedPlaybackFile().then(::toPositionedPlayableFile)
		} finally {
			writeLock.unlock()
		}
	}

	private fun toPositionedPlayableFile(positionedPreparedPlayableFile: PositionedPreparedPlayableFile): PositionedPlayableFile {
		positionedPreparedPlayableFile.preparedPlayableFile
			.bufferingPlaybackFile
			.promiseBufferedPlaybackFile()
			.then { beginQueueingPreparingPlayers() }
		return PositionedPlayableFile(
			positionedPreparedPlayableFile.preparedPlayableFile.playbackHandler,
			positionedPreparedPlayableFile.preparedPlayableFile.playableFileVolumeManager,
			positionedPreparedPlayableFile.positionedFile)
	}

	private fun preparePlayableFileAgainIfNecessary(positionedPreparedPlayableFile: PositionedPreparedPlayableFile): Promise<PositionedPreparedPlayableFile> {
		if (!positionedPreparedPlayableFile.isEmpty) return Promise(positionedPreparedPlayableFile)

		val positionedFile = positionedPreparedPlayableFile.positionedFile

		logger.warn("$positionedFile failed to prepare in time. Cancelling and preparing again.")
		currentPreparingPlaybackHandlerPromise?.preparedPlaybackFilePromise?.cancel()
		return PositionedPreparingFile(
			positionedFile,
			playbackPreparer.promisePreparedPlaybackFile(positionedFile.serviceFile, Duration.ZERO)).also {
				currentPreparingPlaybackHandlerPromise = it
		}.promisePositionedPreparedPlaybackFile()
	}

	private class PositionedPreparingFile(
		override val positionedFile: PositionedFile,
		override val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>)
		: ProvidePreparedPlaybackFile {

		override fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile> =
			preparedPlaybackFilePromise.then(
				{ handler -> PositionedPreparedPlayableFile(positionedFile, handler) },
				{ error -> throw PreparationException(positionedFile, error) })
	}

	private class PositionedUnerringPreparingFile(
		override val positionedFile: PositionedFile,
		override val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>) : ProvidePreparedPlaybackFile {

		override fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile> =
			preparedPlaybackFilePromise.then(
				{ handler -> PositionedPreparedPlayableFile(positionedFile, handler) },
				{ error ->
					logger.warn("An error occurred during preparation, returning an empty handler to trigger re-preparation", error)
					PositionedPreparedPlayableFile.emptyHandler(positionedFile)
				})
	}

	private interface ProvidePreparedPlaybackFile {
		val positionedFile: PositionedFile
		val preparedPlaybackFilePromise: Promise<PreparedPlayableFile>

		fun promisePositionedPreparedPlaybackFile(): Promise<PositionedPreparedPlayableFile>
	}
}
