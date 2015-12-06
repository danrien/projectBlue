package com.lasthopesoftware.bluewater.servers.library.items.playlists.access;

import android.util.SparseArray;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.providers.AbstractCollectionProvider;
import com.lasthopesoftware.threading.FluentTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsProvider extends AbstractCollectionProvider<Playlist> {

    public static final String PlaylistsItemKey = "Playlists";

    private static List<Playlist> cachedPlaylists;
    private static SparseArray<Playlist> mappedPlaylists;
    private static Integer revision;

    private final int playlistId;

    private final ConnectionProvider connectionProvider;

	public PlaylistsProvider(ConnectionProvider connectionProvider) {
		this(connectionProvider, -1);
	}
	
	public PlaylistsProvider(ConnectionProvider connectionProvider, int playlistId) {
		super(connectionProvider, PlaylistsItemKey + "/List");

		this.connectionProvider = connectionProvider;
        this.playlistId = playlistId;
	}

    @Override
    protected List<Playlist> getData(FluentTask<Void, Void, List<Playlist>> task, final HttpURLConnection connection) throws Exception {

        final Integer revision = RevisionChecker.getRevision(connectionProvider);
        if (cachedPlaylists != null && revision.equals(PlaylistsProvider.revision))
            return getPlaylists(playlistId);

        if (task.isCancelled()) return new ArrayList<>();

        final InputStream is = connection.getInputStream();
        try {
            final ArrayList<Playlist> streamResult = PlaylistRequest.GetItems(is);

            int i = 0;
            while (i < streamResult.size()) {
                if (streamResult.get(i).getParent() != null) streamResult.remove(i);
                else i++;
            }

            PlaylistsProvider.revision = revision;
            cachedPlaylists = streamResult;
            mappedPlaylists = null;
            return getPlaylists(playlistId);
        } finally {
            is.close();
        }
    }

    private static List<Playlist> getPlaylists(int playlistId) {
        if (playlistId == -1) return cachedPlaylists;

        if (mappedPlaylists == null) {
            mappedPlaylists = new SparseArray<>(cachedPlaylists.size());
            denormalizeAndMap(cachedPlaylists);
        }

        return mappedPlaylists.get(playlistId).getChildren();
    }

    private static void denormalizeAndMap(List<Playlist> items) {
        for (Playlist playlist : items) {
            mappedPlaylists.append(playlist.getKey(), playlist);
            if (playlist.getChildren().size() > 0) denormalizeAndMap(playlist.getChildren());
        }
    }
}
