package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.volume.specs.fakes.FakeVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 11/12/16.
 */

public class WhenPausingPlayback {

	private IPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = new FakeBufferingPlaybackHandler();

		final Promise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlaybackFile(0, playbackHandler, new ServiceFile(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(preparedPlaybackFileQueue, new FakeVolumeControllerFactory(), 0);

		playlistPlayback.pause();
	}

	@Test
	public void thenPlaybackIsPaused() {
		assertThat(playbackHandler.isPlaying()).isFalse();
	}
}
