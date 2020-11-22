package com.namehillsoftware.projectblue.playback.engine.preparation;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackQueueConfiguration;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.ResponseAction;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayableFile;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;
import com.namehillsoftware.projectblue.playback.file.preparation.queues.IPositionedFileQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PreparedPlayableFileQueue
implements
	ResponseAction<IBufferingPlaybackFile>,
	ImmediateResponse<PositionedPreparedPlayableFile, PositionedPlayableFile>,
	PromisedResponse<PositionedPreparedPlayableFile, PositionedPreparedPlayableFile>,
	Closeable
{
	private static final Logger logger = LoggerFactory.getLogger(PreparedPlayableFileQueue.class);

	private final ReentrantReadWriteLock queueUpdateLock = new ReentrantReadWriteLock();
	private final ConcurrentLinkedQueue<PositionedPreparingFile> bufferingMediaPlayerPromises = new ConcurrentLinkedQueue<>();

	private final IPreparedPlaybackQueueConfiguration configuration;
	private final PlayableFilePreparationSource playbackPreparer;

	private IPositionedFileQueue positionedFileQueue;
	private PositionedPreparingFile currentPreparingPlaybackHandlerPromise;

	public PreparedPlayableFileQueue(IPreparedPlaybackQueueConfiguration configuration, PlayableFilePreparationSource playbackPreparer, IPositionedFileQueue positionedFileQueue) {
		this.configuration = configuration;
		this.playbackPreparer = playbackPreparer;
		this.positionedFileQueue = positionedFileQueue;
	}

	public PreparedPlayableFileQueue updateQueue(IPositionedFileQueue newPositionedFileQueue) {
		final Lock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			final Queue<PositionedPreparingFile> newPositionedPreparingMediaPlayerPromises = new LinkedList<>();

			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null) {
				if (positionedPreparingFile.positionedFile.equals(newPositionedFileQueue.peek())) {
					newPositionedPreparingMediaPlayerPromises.offer(positionedPreparingFile);
					newPositionedFileQueue.poll();
					continue;
				}

				while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null)
					positionedPreparingFile.preparedPlaybackFilePromise.cancel();
			}

			while ((positionedPreparingFile = newPositionedPreparingMediaPlayerPromises.poll()) != null)
				bufferingMediaPlayerPromises.offer(positionedPreparingFile);

			positionedFileQueue = newPositionedFileQueue;

			beginQueueingPreparingPlayers();

			return this;
		} finally {
			writeLock.unlock();
		}
	}

	public Promise<PositionedPlayableFile> promiseNextPreparedPlaybackFile(long preparedAt) {
		currentPreparingPlaybackHandlerPromise = bufferingMediaPlayerPromises.poll();
		if (currentPreparingPlaybackHandlerPromise == null) {
			currentPreparingPlaybackHandlerPromise = getNextPreparingMediaPlayerPromise(preparedAt);
			return currentPreparingPlaybackHandlerPromise != null
				? currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile().then(this)
				: null;
		}

		return
			Promise.whenAny(
				currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile(),
				new Promise<>(PositionedPreparedPlayableFile.emptyHandler(currentPreparingPlaybackHandlerPromise.positionedFile)))
			.eventually(this)
			.then(this);
	}

	private PositionedPreparingFile getNextPreparingMediaPlayerPromise(long preparedAt) {
		final Lock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();

		final PositionedFile positionedFile;
		try {
			positionedFile = positionedFileQueue.poll();
		} finally {
			writeLock.unlock();
		}

		if (positionedFile == null) return null;

		return
			new PositionedPreparingFile(
				positionedFile,
				playbackPreparer.promisePreparedPlaybackFile(positionedFile.getServiceFile(), preparedAt));
	}

	private void beginQueueingPreparingPlayers() {
		final Lock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			if (bufferingMediaPlayerPromises.size() >= configuration.getMaxQueueSize()) return;

			final PositionedPreparingFile nextPreparingMediaPlayerPromise = getNextPreparingMediaPlayerPromise(0);
			if (nextPreparingMediaPlayerPromise == null) return;

			bufferingMediaPlayerPromises.offer(nextPreparingMediaPlayerPromise);

			nextPreparingMediaPlayerPromise.promisePositionedPreparedPlaybackFile().then(this);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() {
		if (currentPreparingPlaybackHandlerPromise != null)
			currentPreparingPlaybackHandlerPromise.preparedPlaybackFilePromise.cancel();

		final Lock writeLock = queueUpdateLock.writeLock();
		writeLock.lock();
		try {
			PositionedPreparingFile positionedPreparingFile;
			while ((positionedPreparingFile = bufferingMediaPlayerPromises.poll()) != null)
				positionedPreparingFile.preparedPlaybackFilePromise.cancel();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public PositionedPlayableFile respond(PositionedPreparedPlayableFile positionedPreparedPlayableFile) {
		positionedPreparedPlayableFile
			.preparedPlayableFile
			.getBufferingPlaybackFile()
			.promiseBufferedPlaybackFile()
			.then(new VoidResponse<>(this));

		return new PositionedPlayableFile(
			positionedPreparedPlayableFile.preparedPlayableFile.getPlaybackHandler(),
			positionedPreparedPlayableFile.preparedPlayableFile.getPlayableFileVolumeManager(),
			positionedPreparedPlayableFile.positionedFile);
	}

	@Override
	public void perform(IBufferingPlaybackFile bufferingPlaybackHandler) {
		beginQueueingPreparingPlayers();
	}

	@Override
	public Promise<PositionedPreparedPlayableFile> promiseResponse(PositionedPreparedPlayableFile positionedPreparedPlayableFile) {
		if (!positionedPreparedPlayableFile.isEmpty())
			return new Promise<>(positionedPreparedPlayableFile);

		final PositionedFile positionedFile = currentPreparingPlaybackHandlerPromise.positionedFile;
		logger.warn(positionedFile + " failed to prepare in time. Cancelling and preparing again.");

		currentPreparingPlaybackHandlerPromise.preparedPlaybackFilePromise.cancel();
		currentPreparingPlaybackHandlerPromise = new PositionedPreparingFile(
			positionedFile,
			playbackPreparer.promisePreparedPlaybackFile(positionedFile.getServiceFile(), 0));

		return currentPreparingPlaybackHandlerPromise.promisePositionedPreparedPlaybackFile();
	}

	private static class PositionedPreparingFile {
		final PositionedFile positionedFile;
		final Promise<PreparedPlayableFile> preparedPlaybackFilePromise;

		private PositionedPreparingFile(PositionedFile positionedFile, Promise<PreparedPlayableFile> preparedPlaybackFilePromise) {
			this.positionedFile = positionedFile;
			this.preparedPlaybackFilePromise = preparedPlaybackFilePromise;
		}

		Promise<PositionedPreparedPlayableFile> promisePositionedPreparedPlaybackFile() {
			return preparedPlaybackFilePromise.then(
				handler -> new PositionedPreparedPlayableFile(positionedFile, handler),
				error -> {
					throw new PreparationException(positionedFile, error);
				});
		}
	}
}
