package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaybackEngine;


import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPlaybackCompletes {

	private static PlaybackEngine playbackEngine;
	private static NowPlaying nowPlaying;
	private static PositionedPlayingFile observedPlayingFile;
	private static PositionedFile resetPositionedFile;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);

		playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		playbackEngine
			.setOnPlayingFileChanged(f -> observedPlayingFile = f)
			.setOnPlaylistReset(f -> resetPositionedFile = f)
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		ResolvablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
		for (int i = 0; i < 4; i ++) {
			final ResolvablePlaybackHandler newPlayingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();
			playingPlaybackHandler.resolve();
			playingPlaybackHandler = newPlayingPlaybackHandler;
		}
		playingPlaybackHandler.resolve();

		nowPlaying = new FuturePromise<>(nowPlayingRepository.getNowPlaying()).get();
	}

	@Test
	public void thenThePlaybackStateIsNotPlaying() {
		assertThat(playbackEngine.isPlaying()).isFalse();
	}

	@Test
	public void thenTheObservedFilePositionIsCorrect() {
		assertThat(observedPlayingFile.asPositionedFile()).isEqualTo(new PositionedFile(4, new ServiceFile(5)));
	}

	@Test
	public void thenTheResetFilePositionIsZero() {
		assertThat(resetPositionedFile).isEqualTo(new PositionedFile(0, new ServiceFile(1)));
	}

	@Test
	public void thenTheSavedFilePositionIsCorrect() {
		assertThat(nowPlaying.filePosition).isEqualTo(0);
	}

	@Test
	public void thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(0);
	}

	@Test
	public void thenTheSavedPlaylistIsCorrect() {
		assertThat(nowPlaying.playlist)
			.containsExactly(new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(3),
				new ServiceFile(4),
				new ServiceFile(5));
	}
}
