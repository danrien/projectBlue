package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.access.ChosenLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/2/16.
 */
public class NowPlayingFileProvider implements INowPlayingFileProvider {

	private final INowPlayingRepository nowPlayingRepository;

	public NowPlayingFileProvider(Context context) {
		this(
			new NowPlayingRepository(
				new SpecificLibraryProvider(
					new ChosenLibraryIdentifierProvider(context).getChosenLibraryId(),
					new LibraryRepository(context)),
				new LibraryRepository(context)));
	}

	public NowPlayingFileProvider(INowPlayingRepository nowPlayingRepository) {
		this.nowPlayingRepository = nowPlayingRepository;
	}

	@Override
	public IPromise<IFile> getNowPlayingFile() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(np -> np.playlist.get(np.playlistPosition));
	}
}
