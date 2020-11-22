package com.namehillsoftware.projectblue.playback.engine.bootstrap;

import com.namehillsoftware.projectblue.playback.engine.ActivePlayer;
import com.namehillsoftware.projectblue.playback.engine.IActivePlayer;
import com.namehillsoftware.projectblue.playback.engine.preparation.PreparedPlayableFileQueue;
import com.namehillsoftware.projectblue.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.projectblue.playback.volume.PlaylistVolumeManager;

import java.io.Closeable;
import java.io.IOException;

public final class PlaylistPlaybackBootstrapper implements IStartPlayback, Closeable {

	private final PlaylistVolumeManager volumeManagement;

	private PlaylistPlayer playlistPlayer;
	private ActivePlayer activePlayer;

	public PlaylistPlaybackBootstrapper(PlaylistVolumeManager volumeManagement) {
		this.volumeManagement = volumeManagement;
	}

	@Override
	public IActivePlayer startPlayback(PreparedPlayableFileQueue preparedPlaybackQueue, final long filePosition) throws IOException {
		close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition);
		activePlayer = new ActivePlayer(playlistPlayer, volumeManagement);

		return activePlayer;
	}

	@Override
	public void close() throws IOException {
		if (activePlayer != null) activePlayer.close();
		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
