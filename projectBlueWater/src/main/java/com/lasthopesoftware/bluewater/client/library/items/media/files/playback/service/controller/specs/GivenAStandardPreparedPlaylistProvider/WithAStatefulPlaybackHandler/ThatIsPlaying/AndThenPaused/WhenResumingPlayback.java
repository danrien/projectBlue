package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused;

import com.lasthopesoftware.bluewater.client.library.items.media.files.File;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.IPlaylistPlayback;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.PlaylistPlayback;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by david on 11/12/16.
 */

public class WhenResumingPlayback {

	private IPlaybackHandler playbackHandler;

	@Before
	public void before() {
		playbackHandler = new StatefulPlaybackHandler();

		final IPromise<PositionedPlaybackFile> positionedPlaybackHandlerContainer =
			new ExpectedPromise<>(() -> new PositionedPlaybackFile(0, playbackHandler, new File(1)));

		final IPreparedPlaybackFileQueue preparedPlaybackFileQueue = mock(IPreparedPlaybackFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer);

		final IPlaylistPlayback playlistPlayback = new PlaylistPlayback(preparedPlaybackFileQueue, 0);

		playlistPlayback.pause();

		playlistPlayback.resume();
	}

	@Test
	public void thenPlaybackIsResumed() {
		assertThat(playbackHandler.isPlaying()).isTrue();
	}
}