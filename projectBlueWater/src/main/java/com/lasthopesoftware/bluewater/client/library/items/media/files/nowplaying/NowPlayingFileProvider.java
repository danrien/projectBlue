package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/2/16.
 */
public class NowPlayingFileProvider implements INowPlayingFileProvider {

	private final INowPlayingRepository nowPlayingRepository;

	public static NowPlayingFileProvider fromActiveLibrary(Context context) {
		final LibraryRepository libraryRepository = new LibraryRepository(context);

		return
			new NowPlayingFileProvider(
				new NowPlayingRepository(
					new SpecificLibraryProvider(
						new SelectedBrowserLibraryIdentifierProvider(context).getSelectedLibraryId(),
						libraryRepository),
					libraryRepository));
	}

	private NowPlayingFileProvider(INowPlayingRepository nowPlayingRepository) {
		this.nowPlayingRepository = nowPlayingRepository;
	}

	@Override
	public IPromise<File> getNowPlayingFile() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(np -> np.playlist.size() > 0 ? np.playlist.get(np.playlistPosition) : null);
	}
}
