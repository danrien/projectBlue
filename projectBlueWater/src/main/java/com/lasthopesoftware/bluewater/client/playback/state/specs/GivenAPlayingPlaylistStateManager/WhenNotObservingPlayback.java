package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/11/17.
 */

public class WhenNotObservingPlayback {
	private static Library library;
	private static PlaylistManager playlistManager;

	@BeforeClass
	public static void context() {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		library = new Library();
		library.setId(1);
		library.setNowPlayingId(5);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			() -> 1,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		playlistManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		final ResolveablePlaybackHandler resolveablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		resolveablePlaybackHandler.resolve();
	}

	@Test
	public void thenTheSavedTrackPositionIsZero() {
		assertThat(library.getNowPlayingId()).isEqualTo(1);
	}

	@Test
	public void thenTheManagerIsPlaying() {
		assertThat(playlistManager.isPlaying()).isTrue();
	}
}
