package com.namehillsoftware.projectblue.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.shared.promises.extensions.ProgressedPromise;

import org.joda.time.Duration;

public class EmptyPlaybackHandler
extends
	ProgressedPromise<Duration, PlayedFile>
implements
	IBufferingPlaybackFile,
	PlayableFile,
	PlayingFile,
	PlayedFile {

	private final Promise<?> promisedThis = this;

	private final int duration;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
		resolve(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Promise<PlayingFile> promisePlayback() {
		return (Promise<PlayingFile>) promisedThis;
	}

	@Override
	public void close() {}

	@Override
	@SuppressWarnings("unchecked")
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return (Promise<IBufferingPlaybackFile>) promisedThis;
	}

	@Override
	public Duration getProgress() {
		return Duration.ZERO;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Promise<PlayableFile> promisePause() {
		return (Promise<PlayableFile>)promisedThis;
	}

	@Override
	public ProgressedPromise<Duration, PlayedFile> promisePlayedFile() {
		return this;
	}

	@Override
	public Duration getDuration() {
		return Duration.millis(duration);
	}
}
