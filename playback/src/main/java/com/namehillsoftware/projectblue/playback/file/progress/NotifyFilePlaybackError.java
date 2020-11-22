package com.lasthopesoftware.bluewater.client.playback.file.progress;

import com.vedsoft.futures.runnables.OneParameterAction;

public interface NotifyFilePlaybackError<T extends Exception> {
	void playbackError(OneParameterAction<T> onError);
}
