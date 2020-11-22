package com.namehillsoftware.projectblue.playback.file.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

public class FakeBufferingPlaybackHandler
implements
	IBufferingPlaybackFile,
	PlayableFile,
	PlayingFile,
	PlayedFile
{
	private boolean isPlaying;
	protected int currentPosition;

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setCurrentPosition(int position) {
		this.currentPosition = position;
	}

	@Override
	public Promise<PlayingFile> promisePlayback() {
		isPlaying = true;
		return new Promise<>(this);
	}

	@Override
	public void close() {

	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}

	public Duration getProgress() {
		return Duration.millis(currentPosition);
	}

	@Override
	public Promise<PlayableFile> promisePause() {
		isPlaying = false;
		return new Promise<>(this);
	}

	@Override
	public ProgressedPromise<Duration, PlayedFile> promisePlayedFile() {
		return new ProgressingPromise<Duration, PlayedFile>() {

			{
				resolve(FakeBufferingPlaybackHandler.this);
			}

			@Override
			public Duration getProgress() {
				return Duration.millis(currentPosition);
			}
		};
	}

	@Override
	public Duration getDuration() {
		return Duration.millis(currentPosition);
	}
}
