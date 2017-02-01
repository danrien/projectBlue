package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.PassThroughPromise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by david on 1/29/17.
 */
public class NowPlayingRepository implements INowPlayingRepository {

	private static final Map<Integer, NowPlaying> nowPlayingCache = new ConcurrentHashMap<>();

	private final Context context;
	private final int libraryId;

	public NowPlayingRepository(Context context, Library library) {
		this.context = context;
		this.libraryId = library.getId();
	}

	@Override
	public IPromise<NowPlaying> getNowPlaying() {
		final NowPlaying cachedNowPlaying = nowPlayingCache.get(libraryId);
		if (cachedNowPlaying != null) {
			return new PassThroughPromise<>(
				new NowPlaying(
					cachedNowPlaying.playlist,
					cachedNowPlaying.playlistPosition,
					cachedNowPlaying.filePosition,
					cachedNowPlaying.isRepeating));
		}

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library ->
					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.then(files ->
							nowPlayingCache.put(
								libraryId,
								new NowPlaying(
									files,
									library.getNowPlayingId(),
									library.getNowPlayingProgress(),
									library.isRepeating()))));
	}

	@Override
	public IPromise<NowPlaying> updateNowPlaying(NowPlaying nowPlaying) {
		nowPlayingCache.put(libraryId, nowPlaying);

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library -> {
					library.setNowPlayingId(nowPlaying.playlistPosition);
					library.setNowPlayingProgress(nowPlaying.filePosition);
					library.setRepeating(nowPlaying.isRepeating);

					return
						FileStringListUtilities
							.promiseSerializedFileStringList(nowPlaying.playlist)
							.thenPromise(serializedPlaylist -> {
								library.setSavedTracksString(serializedPlaylist);

								return LibrarySession.saveLibrary(context, library);
							})
							.then(savedLibrary -> nowPlaying);
				});
	}
}
