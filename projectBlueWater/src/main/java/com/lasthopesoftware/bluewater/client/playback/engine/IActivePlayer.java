package com.lasthopesoftware.bluewater.client.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;

import io.reactivex.observables.ConnectableObservable;

public interface IActivePlayer {
	void pause();
	void resume();
	ConnectableObservable<PositionedPlaybackFile> observe();
}