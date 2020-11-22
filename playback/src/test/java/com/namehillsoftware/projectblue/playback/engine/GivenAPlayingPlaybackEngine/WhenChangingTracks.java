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
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTracks {

	private static PositionedFile nextSwitchedFile;
	private static PositionedPlayingFile latestFile;

	private static final List<PositionedPlayingFile> startedFiles = new ArrayList<>();

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		final Library library = new Library();
		library.setId(1);

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaybackEngine playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		final CountDownLatch countDownLatch = new CountDownLatch(2);

		playbackEngine
			.setOnPlayingFileChanged(p -> {
				startedFiles.add(p);
				latestFile = p;
				countDownLatch.countDown();
			})
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		final ResolvablePlaybackHandler playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve();

		playbackEngine.changePosition(3, 0).then(p -> {
			nextSwitchedFile = p;
			countDownLatch.countDown();
			return null;
		});

		fakePlaybackPreparerProvider.deferredResolution.resolve();
		playingPlaybackHandler.resolve();

		countDownLatch.await();
	}

	@Test
	public void thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheLatestObservedFileIsAtTheCorrectTrackPosition() {
		assertThat(latestFile.getPlaylistPosition()).isEqualTo(3);
	}

	@Test
	public void thenTheFirstStartedFileIsCorrect() {
		assertThat(startedFiles.get(0).asPositionedFile()).isEqualTo(new PositionedFile(0, new ServiceFile(1)));
	}

	@Test
	public void thenTheChangedStartedFileIsCorrect() {
		assertThat(startedFiles.get(1).asPositionedFile()).isEqualTo(new PositionedFile(3, new ServiceFile(4)));
	}

	@Test
	public void thenThePlaylistIsStartedTwice() {
		assertThat(startedFiles).hasSize(2);
	}
}
