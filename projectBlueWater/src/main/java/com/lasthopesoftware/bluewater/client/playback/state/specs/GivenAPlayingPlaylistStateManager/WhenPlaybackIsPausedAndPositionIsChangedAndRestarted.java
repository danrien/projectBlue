package com.lasthopesoftware.bluewater.client.playback.state.specs.GivenAPlayingPlaylistStateManager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.state.PlaylistManager;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 3/12/17.
 */

public class WhenPlaybackIsPausedAndPositionIsChangedAndRestarted {

	private static PlaylistManager playlistManager;
	private static NowPlaying nowPlaying;
	private static List<PositionedPlaybackFile> positionedFiles = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException {
		final FakeDeferredPlaybackPreparerProvider fakePlaybackPreparerProvider = new FakeDeferredPlaybackPreparerProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);

		playlistManager = new PlaylistManager(
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		playlistManager
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0)
			.then(obs -> obs.subscribe(f -> positionedFiles.add(f)));

		fakePlaybackPreparerProvider.deferredResolution.resolve().resolve();
		fakePlaybackPreparerProvider.deferredResolution.resolve();

		playlistManager.pause();

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		playlistManager
			.skipToNext()
			.eventually(p -> playlistManager.skipToNext())
			.eventually(p -> playlistManager.resume())
			.then(obs -> fakePlaybackPreparerProvider.deferredResolution.resolve())
			.eventually(res -> nowPlayingRepository.getNowPlaying())
			.then(np -> {
				nowPlaying = np;
				countDownLatch.countDown();
				return null;
			});

		countDownLatch.await();
	}

	@Test
	public void thenThePlaybackStateIsPlaying() {
		assertThat(playlistManager.isPlaying()).isTrue();
	}

	@Test
	public void thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(3);
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

	@Test
	public void thenTheObservedFileIsCorrect() {
		assertThat(positionedFiles.get(positionedFiles.size() - 1).getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheFirstSkippedFileIsOnlyObservedOnce() {
		assertThat(
			Stream.of(positionedFiles)
				.map(PositionedPlaybackFile::asPositionedFile)
				.collect(Collectors.toList()))
			.containsOnlyOnce(new PositionedFile(1, new ServiceFile(2)));
	}

	@Test
	public void thenTheSecondSkippedFileIsNotObserved() {
		assertThat(
			Stream.of(positionedFiles)
				.map(PositionedPlaybackFile::asPositionedFile)
				.collect(Collectors.toList()))
			.doesNotContain(new PositionedFile(2, new ServiceFile(3)));
	}
}
