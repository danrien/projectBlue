package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;

/**
 * Created by david on 1/17/17.
 */
public interface IPlaybackBroadcaster {
    void sendPlaybackBroadcast(String broadcastMessage, int libraryId, PositionedPlaybackServiceFile positionedPlaybackFile);

}
