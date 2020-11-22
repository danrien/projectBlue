package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;

public class LocalPlaybackBroadcaster implements IPlaybackBroadcaster {
    private final LocalBroadcastManager localBroadcastManager;

    public LocalPlaybackBroadcaster(LocalBroadcastManager localBroadcastManager) {
        this.localBroadcastManager = localBroadcastManager;
    }

    @Override
    public void sendPlaybackBroadcast(final String broadcastMessage, final LibraryId libraryId, final PositionedFile positionedFile) {
        final Intent playbackBroadcastIntent = new Intent(broadcastMessage);

        final int currentPlaylistPosition = positionedFile.getPlaylistPosition();

        final int fileKey = positionedFile.getServiceFile().getKey();

        playbackBroadcastIntent
			.putExtra(PlaylistEvents.PlaylistParameters.playlistPosition, currentPlaylistPosition)
			.putExtra(PlaylistEvents.PlaybackFileParameters.fileLibraryId, libraryId.getId())
			.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, fileKey);

        localBroadcastManager.sendBroadcast(playbackBroadcastIntent);
    }
}
