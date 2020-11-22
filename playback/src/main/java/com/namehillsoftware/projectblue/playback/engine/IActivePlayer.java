package com.namehillsoftware.projectblue.playback.engine;


import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;

import io.reactivex.observables.ConnectableObservable;

public interface IActivePlayer {
	Promise<?> pause();
	Promise<PositionedPlayingFile> resume();
	ConnectableObservable<PositionedPlayingFile> observe();
}
