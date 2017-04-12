package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.promises.Promise;

import java.util.List;

public interface IPausedPlaylist extends IPlaylistTrackChanger {
	Promise<IStartedPlaylist> startPlaylist(final List<ServiceFile> playlist, final int playlistPosition, final int filePosition);
	Promise<IStartedPlaylist> resume();
}
