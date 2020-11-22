package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaybackEngine;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.specs.fakes.FakeDeferredPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class WhenRemovingTheCurrentlyPlayingFile {

	private static final CompletingFileQueueProvider fileQueueProvider = spy(new CompletingFileQueueProvider());
	private static final Library library = new Library();
	private static PositionedPlayingFile positionedPlayingFile;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException, TimeoutException {
		final FakeDeferredPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new FakeDeferredPlayableFilePreparationSourceProvider();

		library.setId(1);
		library.setSavedTracksString(new FuturePromise<>(FileStringListUtilities.promiseSerializedFileStringList(Arrays.asList(
			new ServiceFile(1),
			new ServiceFile(2),
			new ServiceFile(3),
			new ServiceFile(4),
			new ServiceFile(5),
			new ServiceFile(13),
			new ServiceFile(27)))).get());
		library.setNowPlayingId(5);

		final ISpecificLibraryProvider libraryProvider = () -> new Promise<>(library);
		final ILibraryStorage libraryStorage = new ILibraryStorage() {
			@NotNull
			@Override
			public Promise<Library> saveLibrary(@NotNull Library library) {
				return new Promise<>(library);
			}

			@NotNull
			@Override
			public Promise<Object> removeLibrary(@NotNull Library library) {
				return Promise.empty();
			}
		};

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = mock(IFilePropertiesContainerRepository.class);
		when(filePropertiesContainerRepository.getFilePropertiesContainer(new UrlKeyHolder<>("", new ServiceFile(5))))
			.thenReturn(new FilePropertiesContainer(1, new HashMap<String, String>() {{
				put(KnownFileProperties.DURATION, "100");
			}}));

		final PlaybackEngine playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				() -> 1),
			Collections.singletonList(fileQueueProvider),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		new FuturePromise<>(playbackEngine.resume()).get(1, TimeUnit.SECONDS);

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		playbackEngine.setOnPlayingFileChanged(c -> positionedPlayingFile = c);

		final FuturePromise<NowPlaying> futurePlaying = new FuturePromise<>(playbackEngine.removeFileAtPosition(5));

		fakePlaybackPreparerProvider.deferredResolution.resolve();

		futurePlaying.get(1, TimeUnit.SECONDS);
	}

	@Test
	public void thenTheCurrentlyPlayingFilePositionIsTheSame() {
		assertThat(library.getNowPlayingId()).isEqualTo(5);
	}

	@Test
	public void thenTheFileQueueIsShiftedToTheNextFile() {
		assertThat(positionedPlayingFile.getServiceFile()).isEqualTo(new ServiceFile(27));
	}
}
