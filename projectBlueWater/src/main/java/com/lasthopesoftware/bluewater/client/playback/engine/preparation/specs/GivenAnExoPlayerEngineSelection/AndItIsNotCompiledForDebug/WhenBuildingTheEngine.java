package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAnExoPlayerEngineSelection.AndItIsNotCompiledForDebug;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preferences.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.preparation.MediaPlayerPlayableFilePreparationSourceProvider;

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
		when(lookupSelectedPlaybackEngineType.getSelectedPlaybackEngineType())
			.thenReturn(PlaybackEngineType.ExoPlayer);

		final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
			new PreparedPlaybackQueueFeederBuilder(
				mock(Context.class),
				mock(BestMatchUriProvider.class),
				mock(RemoteFileUriProvider.class),
				lookupSelectedPlaybackEngineType,
				() -> false);

		engine = playbackEngineBuilder.build(new Library());
	}

	@Test
	public void thenAMediaPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(MediaPlayerPlayableFilePreparationSourceProvider.class);
	}
}
