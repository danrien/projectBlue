package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.playback;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;

import org.joda.time.Duration;

import java.io.EOFException;

public class PromisedPlayedExoPlayer
extends
	ProgressingPromise<Duration, PlayedFile>
implements
	PlayedFile,
	Player.EventListener {

	private final ExoPlayer exoPlayer;
	private final ExoPlayerPlaybackHandler handler;
	private Duration fileProgress = Duration.ZERO;

	public PromisedPlayedExoPlayer(ExoPlayer exoPlayer, ExoPlayerPlaybackHandler handler) {
		this.exoPlayer = exoPlayer;
		this.handler = handler;
		this.exoPlayer.addListener(this);
	}

	@Override
	public Duration getProgress() {
		if (!exoPlayer.getPlayWhenReady()) return fileProgress;

		final long currentPosition = exoPlayer.getCurrentPosition();

		return currentPosition != fileProgress.getMillis()
			? (fileProgress = Duration.millis(currentPosition))
			: fileProgress;
	}

	@Override
	public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

	}

	@Override
	public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

	}

	@Override
	public void onLoadingChanged(boolean isLoading) {

	}

	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		if (playbackState != Player.STATE_ENDED) return;

		removeListener();
		resolve(this);
	}

	@Override
	public void onRepeatModeChanged(int repeatMode) {

	}

	@Override
	public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

	}

	@Override
	public void onPlayerError(ExoPlaybackException error) {
		if (error.getCause() instanceof EOFException) {
			removeListener();
			resolve(this);
			return;
		}

		removeListener();
		reject(new ExoPlayerException(handler, error));
	}

	@Override
	public void onPositionDiscontinuity(int reason) {

	}

	@Override
	public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

	}

	@Override
	public void onSeekProcessed() {

	}

	private void removeListener() {
		exoPlayer.removeListener(this);
	}
}
