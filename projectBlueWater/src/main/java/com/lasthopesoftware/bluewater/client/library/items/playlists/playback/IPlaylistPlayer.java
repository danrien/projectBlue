package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.shared.IUsefulObservable;

import java.io.Closeable;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayer extends IUsefulObservable<PositionedPlaybackFile>, Closeable {
	void pause();
	void resume();

	void setVolume(float volume);

	void cancel();
}
