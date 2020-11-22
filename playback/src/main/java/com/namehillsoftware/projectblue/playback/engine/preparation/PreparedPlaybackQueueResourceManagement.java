package com.namehillsoftware.projectblue.playback.engine.preparation;


import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackQueueConfiguration;
import com.namehillsoftware.projectblue.playback.file.preparation.queues.IPositionedFileQueue;

public class PreparedPlaybackQueueResourceManagement implements ManagePlaybackQueues {
	private final IPlayableFilePreparationSourceProvider playbackPreparerProvider;
	private final IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration;

	private PreparedPlayableFileQueue preparedPlaybackQueue;

	public PreparedPlaybackQueueResourceManagement(IPlayableFilePreparationSourceProvider playbackPreparerProvider, IPreparedPlaybackQueueConfiguration preparedPlaybackQueueConfiguration) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.preparedPlaybackQueueConfiguration = preparedPlaybackQueueConfiguration;
	}

	@Override
	public PreparedPlayableFileQueue initializePreparedPlaybackQueue(IPositionedFileQueue positionedFileQueue) {
		close();

		return preparedPlaybackQueue =
			new PreparedPlayableFileQueue(
				preparedPlaybackQueueConfiguration,
				this.playbackPreparerProvider.providePlayableFilePreparationSource(),
				positionedFileQueue);
	}

	@Override
	public boolean tryUpdateQueue(IPositionedFileQueue positionedFileQueue) {
		if (preparedPlaybackQueue == null) return false;

		preparedPlaybackQueue.updateQueue(positionedFileQueue);
		return true;
	}

	@Override
	public void close() {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();
	}
}
