package com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.GivenTheSetOfPlaybackEngines;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup;
import com.namehillsoftware.projectblue.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenLookingUpTheDefaultEngine {

	private static PlaybackEngineType playbackEngineType;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		playbackEngineType = new FuturePromise<>(new DefaultPlaybackEngineLookup().promiseDefaultEngineType()).get();
	}

	@Test
	public void thenItIsExoPlayer() {
		assertThat(playbackEngineType).isEqualTo(PlaybackEngineType.ExoPlayer);
	}
}
