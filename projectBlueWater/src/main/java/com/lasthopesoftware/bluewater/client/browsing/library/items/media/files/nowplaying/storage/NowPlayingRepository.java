package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.nowplaying.storage;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NowPlayingRepository implements INowPlayingRepository {

	private static final Map<Integer, NowPlaying> nowPlayingCache = new ConcurrentHashMap<>();

	private final ISpecificLibraryProvider libraryProvider;
	private final ILibraryStorage libraryRepository;

	private volatile int libraryId = -1;

	public NowPlayingRepository(ISpecificLibraryProvider libraryProvider, ILibraryStorage libraryRepository) {
		this.libraryProvider = libraryProvider;
		this.libraryRepository = libraryRepository;
	}

	@Override
	public Promise<NowPlaying> getNowPlaying() {
		if (nowPlayingCache.containsKey(libraryId))
			return new Promise<>(nowPlayingCache.get(libraryId));

		return
			libraryProvider
				.getLibrary()
				.eventually(library -> {
					libraryId = library.getId();

					final String savedTracksString = library.getSavedTracksString();
					if (savedTracksString == null || savedTracksString.isEmpty()) {
						final NowPlaying nowPlaying =
							new NowPlaying(
								library.getNowPlayingId(),
								library.getNowPlayingProgress(),
								library.isRepeating());

						nowPlayingCache.put(libraryId, nowPlaying);

						return new Promise<>(nowPlaying);
					}

					return
						FileStringListUtilities
							.promiseParsedFileStringList(savedTracksString)
							.then(files -> {
								final NowPlaying nowPlaying =
									new NowPlaying(
										files instanceof List ? (List<ServiceFile>)files : new ArrayList<>(files),
										library.getNowPlayingId(),
										library.getNowPlayingProgress(),
										library.isRepeating());

								nowPlayingCache.put(libraryId, nowPlaying);

								return nowPlaying;
							});
				});
	}

	@Override
	public Promise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying) {
		if (libraryId < 0)
			return getNowPlaying().eventually(np -> updateNowPlaying(nowPlaying));

		nowPlayingCache.put(libraryId, nowPlaying);

		libraryProvider
			.getLibrary()
			.eventually(library -> {
				library.setNowPlayingId(nowPlaying.playlistPosition);
				library.setNowPlayingProgress(nowPlaying.filePosition);
				library.setRepeating(nowPlaying.isRepeating);

				return
					FileStringListUtilities
						.promiseSerializedFileStringList(nowPlaying.playlist)
						.eventually(serializedPlaylist -> {
							library.setSavedTracksString(serializedPlaylist);

							return libraryRepository.saveLibrary(library);
						});
			});

		return new Promise<>(nowPlaying);
	}
}
