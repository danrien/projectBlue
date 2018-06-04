package com.lasthopesoftware.bluewater.client.playback.engine.selection.specs.GivenASavedExoPlayerPlaybackEngineType;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class WhenGettingThePlaybackEngineType {

	private PlaybackEngineType playbackEngineType;

	@Before
	public void before() throws ExecutionException, InterruptedException {
		final PlaybackEngineTypeSelectionPersistence playbackEngineTypeSelectionPersistence =
			new PlaybackEngineTypeSelectionPersistence(
				RuntimeEnvironment.application,
				mock(PlaybackEngineTypeChangedBroadcaster.class));

		playbackEngineTypeSelectionPersistence.selectPlaybackEngine(PlaybackEngineType.ExoPlayer);

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(
				RuntimeEnvironment.application,
				Promise::empty);

		playbackEngineType = new FuturePromise<>(selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()).get();
	}

	@Test
	public void thenThePlaybackEngineTypeIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
