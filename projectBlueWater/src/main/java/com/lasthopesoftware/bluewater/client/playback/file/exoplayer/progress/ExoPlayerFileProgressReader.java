package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress;

import com.google.android.exoplayer2.ExoPlayer;
import com.namehillsoftware.projectblue.playback.file.progress.ReadFileProgress;

import org.joda.time.Duration;

public class ExoPlayerFileProgressReader implements ReadFileProgress {

	private final ExoPlayer exoPlayer;

	private Duration fileProgress = Duration.ZERO;

	public ExoPlayerFileProgressReader(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
	}

	@Override
	public synchronized Duration getProgress() {
		if (!exoPlayer.getPlayWhenReady()) return fileProgress;

		final long currentPosition = exoPlayer.getCurrentPosition();

		return currentPosition != fileProgress.getMillis()
			? (fileProgress = Duration.millis(currentPosition))
			: fileProgress;
	}
}
