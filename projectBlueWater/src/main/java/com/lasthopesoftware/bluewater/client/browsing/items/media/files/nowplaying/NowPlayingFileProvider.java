package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider;
import com.namehillsoftware.handoff.promises.Promise;

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
	public Promise<ServiceFile> getNowPlayingFile() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(np -> np.playlist.size() > 0 ? np.playlist.get(np.playlistPosition) : null);
	}
}
