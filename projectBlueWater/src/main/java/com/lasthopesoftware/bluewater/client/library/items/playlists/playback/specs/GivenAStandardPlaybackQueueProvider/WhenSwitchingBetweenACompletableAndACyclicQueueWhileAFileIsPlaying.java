package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPlaybackQueueProvider;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.BufferingPlaybackQueuesProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayerManager;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayerManager;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.ResolveablePlaybackHandler;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by david on 12/22/16.
 */

public class WhenSwitchingBetweenACompletableAndACyclicQueueWhileAFileIsPlaying {

	private static IPlaybackHandler playbackHandler;
	private static IPlaybackHandler expectedPlaybackHandler;
	private static ArrayList<PositionedPlaybackFile> playedFiles;
	private static ArrayList<Integer> expectedGeneratedFileStream;

	@BeforeClass
	public static void before() {

		final Map<IFile, ResolveablePlaybackHandler> resolveablePlaybackHandlers = new HashMap<>();

		final Random random = new Random();

		int numFiles;
		while ((numFiles = random.nextInt(10)) < 10);

		int generatedSwitchPoint;
		while ((generatedSwitchPoint = random.nextInt(numFiles)) <= 0);

		final int switchPoint = generatedSwitchPoint;

		final PlaylistPlayerManager playlistPlayerProducer =
			new PlaylistPlayerManager(new BufferingPlaybackQueuesProvider((file, preparedAt) -> {
				if (!resolveablePlaybackHandlers.containsKey(file)) {
					final ResolveablePlaybackHandler resolveablePlaybackHandler = spy(new ResolveablePlaybackHandler());
					if (file.getKey() == switchPoint)
						playbackHandler = resolveablePlaybackHandler;

					resolveablePlaybackHandlers.put(file, resolveablePlaybackHandler);
				}

				return new MockResolveAction(resolveablePlaybackHandlers.get(file));
			}));


		final IPlaylistPlayerManager playlistPlayerManager = playlistPlayerProducer.startAsCompletable(Stream.range(1, numFiles).map(File::new).collect(Collectors.toList()), 0, 0);

		playedFiles = new ArrayList<>();

		playlistPlayerManager.subscribe(positionedPlaybackFile -> playedFiles.add(positionedPlaybackFile));

		for (int i = 1; i < generatedSwitchPoint; i++) {
			resolveablePlaybackHandlers.get(new File(i)).resolve();
		}

		playlistPlayerManager.continueAsCyclical();

		int iterations;
		while ((iterations = random.nextInt(100)) <= 0);

		expectedGeneratedFileStream = new ArrayList<>(iterations * numFiles);

		for (int j = 0; j <= iterations; j++) {
			for (int i = 1; i < numFiles; i++) {
				if (j >= iterations && i >= switchPoint) {
					expectedGeneratedFileStream.add(i);
					break;
				}

				resolveablePlaybackHandlers.get(new File(i)).resolve();

				expectedGeneratedFileStream.add(i);
			}
		}
	}

	@Test
	public void thenThePlaybackOfTheCurrentFileIsNeverPaused() {
		verify(playbackHandler, times(0)).pause();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsNeverClosed() throws IOException {
		verify(playbackHandler, times(0)).close();
	}

	@Test
	public void thenTheCurrentPlaybackHandlerIsPlaying() {
		assertThat(playbackHandler.isPlaying()).isTrue();
	}
//
//	@Test
//	public void thenThePlaybackHandlerRemainsTheSame() {
//		assertThat(playbackHandler).isEqualTo(expectedPlaybackHandler);
//	}

	@Test
	public void thenTheNewPlaybackQueuePlaysUntilCompletion() {
		assertThat(Stream.of(playedFiles).map(File::getKey).collect(Collectors.toList())).containsExactlyElementsOf(expectedGeneratedFileStream);
	}

	private static class MockResolveAction implements ThreeParameterAction<IResolvedPromise<IBufferingPlaybackHandler>, IRejectedPromise, OneParameterAction<Runnable>> {

		final IBufferingPlaybackHandler resolveablePlaybackHandler;

		private MockResolveAction(IBufferingPlaybackHandler resolveablePlaybackHandler) {
			this.resolveablePlaybackHandler = resolveablePlaybackHandler;
		}

		@Override
		public void runWith(IResolvedPromise<IBufferingPlaybackHandler> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			resolve.withResult(resolveablePlaybackHandler);
		}
	}
}
