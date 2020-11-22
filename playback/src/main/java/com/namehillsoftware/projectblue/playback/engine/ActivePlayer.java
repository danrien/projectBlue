package com.namehillsoftware.projectblue.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;
import com.namehillsoftware.projectblue.playback.volume.PlaylistVolumeManager;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public class ActivePlayer implements IActivePlayer, AutoCloseable {

	private final Disposable fileChangedObservableConnection;
	private final IPlaylistPlayer playlistPlayer;
	private final ConnectableObservable<PositionedPlayingFile> observableProxy;

	public ActivePlayer(IPlaylistPlayer playlistPlayer, PlaylistVolumeManager volumeManagement) {
		this.playlistPlayer = playlistPlayer;

		volumeManagement.managePlayer(playlistPlayer);

		observableProxy = Observable.create(playlistPlayer).replay(1);

		fileChangedObservableConnection = observableProxy.connect();
	}

	@Override
	public Promise<?> pause() {
		return playlistPlayer.pause();
	}

	@Override
	public Promise<PositionedPlayingFile> resume() {
		return playlistPlayer.resume();
	}

	@Override
	public ConnectableObservable<PositionedPlayingFile> observe() {
		return observableProxy;
	}

	@Override
	public void close() {
		fileChangedObservableConnection.dispose();
	}
}
