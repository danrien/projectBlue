package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;
import com.namehillsoftware.projectblue.playback.playlist.PlaylistPlayer;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenResumingPlayback {

	private FakeBufferingPlaybackHandler playbackHandler;
	private PositionedPlayingFile playingFile;

	@Before
	public void before() throws ExecutionException, InterruptedException {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new NoTransformVolumeManager(),
				new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, 0);

		Observable.create(playlistPlayback).subscribe();

		new FuturePromise<>(playlistPlayback.pause()).get();

		playingFile = new FuturePromise<>(playlistPlayback.resume()).get();
	}

	@Test
	public void thenPlaybackIsResumed() {
		assertThat(playbackHandler.isPlaying()).isTrue();
	}

	@Test
	public void thenThePlaylistPositionIsCorrect() {
		assertThat(playingFile.getPlaylistPosition()).isEqualTo(0);
	}

	@Test
	public void thenThePlayingFileIsCorrect() {
		assertThat(playingFile.getServiceFile()).isEqualTo(new ServiceFile(1));
	}
}
