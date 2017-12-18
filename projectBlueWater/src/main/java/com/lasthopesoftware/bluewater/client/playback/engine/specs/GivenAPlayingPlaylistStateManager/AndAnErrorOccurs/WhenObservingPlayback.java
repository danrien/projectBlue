package com.lasthopesoftware.bluewater.client.playback.engine.specs.GivenAPlayingPlaylistStateManager.AndAnErrorOccurs;

import com.lasthopesoftware.bluewater.client.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PlayableFilePreparationSource;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenObservingPlayback {

	private static Throwable error;

	@BeforeClass
	public static void context() {
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

		final ISpecificLibraryProvider libraryProvider = mock(ISpecificLibraryProvider.class);
		when(libraryProvider.getLibrary()).thenReturn(new Promise<>(library));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.saveLibrary(any())).thenReturn(new Promise<>(library));

		final PlaybackEngine playbackEngine = new PlaybackEngine(
			fakePlaybackPreparerProvider,
			fakePlaybackPreparerProvider,
			Collections.singletonList(new CompletingFileQueueProvider()),
			new NowPlayingRepository(libraryProvider, libraryStorage),
			new PlaylistPlaybackBootstrapper(new PlaylistVolumeManager(1.0f), mock(IPlaybackHandlerVolumeControllerFactory.class)));

		playbackEngine
			.setOnPlaylistError(e -> error = e)
			.startPlaylist(
				Arrays.asList(
					new ServiceFile(1),
					new ServiceFile(2),
					new ServiceFile(3),
					new ServiceFile(4),
					new ServiceFile(5)), 0, 0);

		deferredErrorPlaybackPreparer.resolve();
	}

	@Test
	public void thenTheErrorIsBroadcast() {
		assertThat(error).isNotNull();
	}

	private static class DeferredErrorPlaybackPreparer implements PlayableFilePreparationSource {

		private Messenger<PreparedPlayableFile> reject;

		void resolve() {
			if (reject != null)
				reject.sendRejection(new Exception());
		}

		@Override
		public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
			return new Promise<>(messenger -> reject = messenger);
		}
	}
}
