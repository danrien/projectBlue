package com.lasthopesoftware.bluewater.client.playback.file.initialization;

import android.net.Uri;

import java.io.IOException;

public interface IPlaybackInitialization<TMediaPlayer> {
	TMediaPlayer initializeMediaPlayer(Uri fileUri) throws IOException;
}
