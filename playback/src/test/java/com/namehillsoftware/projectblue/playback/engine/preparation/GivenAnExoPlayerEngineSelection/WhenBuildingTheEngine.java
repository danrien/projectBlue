package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAnExoPlayerEngineSelection;

import android.content.Context;
import android.os.Handler;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.SpawnMediaSources;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingTheEngine {

	private static Object engine;

	@BeforeClass
	public static void before() {
		final LookupSelectedPlaybackEngineType lookupSelectedPlaybackEngineType =
			mock(LookupSelectedPlaybackEngineType.class);
		when(lookupSelectedPlaybackEngineType.promiseSelectedPlaybackEngineType())
			.thenReturn(new Promise<>(PlaybackEngineType.ExoPlayer));

		final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
			new PreparedPlaybackQueueFeederBuilder(
				mock(Context.class),
				mock(Handler.class),
				mock(SpawnMediaSources.class),
				mock(BestMatchUriProvider.class));

		engine = playbackEngineBuilder.build(new Library());
	}

	@Test
	public void thenAnExoPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(ExoPlayerPlayableFilePreparationSourceProvider.class);
	}
}
