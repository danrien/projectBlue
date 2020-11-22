package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressedPromise;
import com.namehillsoftware.handoff.Messenger;

import org.jetbrains.annotations.Nullable;
import org.joda.time.Duration;

public class ResolvablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private final ProgressedPromise<Duration, PlayedFile> promise = new ProgressedPromise<Duration, PlayedFile>((messenger) -> this.resolve = messenger) {
		@Nullable
		@Override
		public Duration getProgress() {
			return Duration.millis(currentPosition);
		}
	};

	private Messenger<PlayedFile> resolve;

	@Override
	public ProgressedPromise<Duration, PlayedFile> promisePlayedFile() {
		return promise;
	}

	public void resolve() {
		if (resolve != null)
			resolve.sendResolution(this);

		resolve = null;
	}
}
