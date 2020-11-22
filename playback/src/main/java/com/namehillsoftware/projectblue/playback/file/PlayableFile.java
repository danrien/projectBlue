package com.namehillsoftware.projectblue.playback.file;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.progress.ReadFileProgress;

import java.io.Closeable;

public interface PlayableFile extends ReadFileProgress, Closeable {
	Promise<PlayingFile> promisePlayback();
}
