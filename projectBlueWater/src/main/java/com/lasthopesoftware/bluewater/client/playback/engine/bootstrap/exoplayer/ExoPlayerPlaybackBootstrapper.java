package com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.IStartPlayback;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;

import java.io.Closeable;

public class ExoPlayerPlaybackBootstrapper implements IStartPlayback, Closeable {
	private final CreateExoPlayers exoPlayerCreator;

	private ExoPlayer exoPlayer;

	public ExoPlayerPlaybackBootstrapper(CreateExoPlayers exoPlayerCreator) {
		this.exoPlayerCreator = exoPlayerCreator;
	}

	@Override
	public synchronized IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, long filePosition) {
		close();

		exoPlayer = exoPlayerCreator.createExoPlayer();

		return new ActiveExoPlaylistPlayer(exoPlayer);
	}

	@Override
	public void close() {
		if (exoPlayer != null)
			exoPlayer.release();
	}
}
