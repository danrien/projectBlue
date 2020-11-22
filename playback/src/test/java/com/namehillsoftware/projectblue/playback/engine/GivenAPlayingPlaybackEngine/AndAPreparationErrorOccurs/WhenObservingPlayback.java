package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaybackEngine.AndAPreparationErrorOccurs;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakePreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class WhenObservingPlayback {

	private static PreparationException error;
	private static NowPlaying nowPlaying;

	@BeforeClass
	public static void context() throws InterruptedException, ExecutionException {
		final DeferredErrorPlaybackPreparer deferredErrorPlaybackPreparer = new DeferredErrorPlaybackPreparer();

		final IPlayableFilePreparationSourceProvider fakePlaybackPreparerProvider = new IPlayableFilePreparationSourceProvider() {
			@Override
			public PlayableFilePreparationSource providePlayableFilePreparationSource() {
				return deferredErrorPlaybackPreparer;
			}

			@Override
			public int getMaxQueueSize() {
				return 1;
			}
		};

		final Library library = new Library();
		library.setId(1);

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

		final NowPlayingRepository nowPlayingRepository = new NowPlayingRepository(libraryProvider, libraryStorage);
		final PlaybackEngine playbackEngine = new FuturePromise<>(PlaybackEngine.createEngine(
			new PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider,
				fakePlaybackPreparerProvider),
			Collections.singletonList(new CompletingFileQueueProvider()),
			nowPlayingRepository,
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f)))).get();

		new FuturePromise<>(playbackEngine
			.setOnPlaylistError(e -> {
				if (e instanceof PreparationException)
					error = (PreparationException)e;
			})
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)),
				0,
				0))
			.get();

		deferredErrorPlaybackPreparer.resolve().resolve();
		deferredErrorPlaybackPreparer.reject();

		nowPlaying = new FuturePromise<>(nowPlayingRepository.getNowPlaying()).get();
	}

	@Test
	public void thenTheErrorIsBroadcast() {
		assertThat(error).isNotNull();
	}

	@Test
	public void thenTheFilePositionIsSaved() {
		assertThat(nowPlaying.playlistPosition).isEqualTo(1);
	}

	private static class DeferredErrorPlaybackPreparer implements PlayableFilePreparationSource {

		private Messenger<PreparedPlayableFile> messenger;

		ResolvablePlaybackHandler resolve() {
			final ResolvablePlaybackHandler playbackHandler = new ResolvablePlaybackHandler();
			if (messenger != null)
				messenger.sendResolution(new FakePreparedPlayableFile<>(playbackHandler));

			return playbackHandler;
		}

		void reject() {
			if (messenger != null)
				messenger.sendRejection(new Exception());
		}

		@Override
		public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
			return new Promise<>(messenger -> this.messenger = messenger);
		}
	}
}
