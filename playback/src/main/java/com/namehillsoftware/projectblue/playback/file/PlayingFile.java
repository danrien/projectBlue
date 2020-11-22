package com.namehillsoftware.projectblue.playback.file;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.progress.ReadFileDuration;
import com.namehillsoftware.projectblue.shared.promises.extensions.ProgressedPromise;

import org.joda.time.Duration;

public interface PlayingFile extends ReadFileDuration {
	Promise<PlayableFile> promisePause();

	ProgressedPromise<Duration, PlayedFile> promisePlayedFile();
}
