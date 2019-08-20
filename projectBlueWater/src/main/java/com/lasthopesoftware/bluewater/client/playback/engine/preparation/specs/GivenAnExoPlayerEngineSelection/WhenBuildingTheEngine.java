package com.lasthopesoftware.bluewater.client.playback.engine.preparation.specs.GivenAnExoPlayerEngineSelection;

import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.RenderersFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.QueueMediaSources;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.LookupSelectedPlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenBuildingTheEngine {

	private static Object engine;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final LookupSelectedPlaybackEngineType lookupSelectedPlaybackEngineType =
			mock(LookupSelectedPlaybackEngineType.class);
		when(lookupSelectedPlaybackEngineType.promiseSelectedPlaybackEngineType())
			.thenReturn(new Promise<>(PlaybackEngineType.ExoPlayer));

		final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
			new PreparedPlaybackQueueFeederBuilder(
				lookupSelectedPlaybackEngineType,
				mock(Handler.class),
				mock(BestMatchUriProvider.class),
				mock(MediaSourceProvider.class),
				mock(ExoPlayer.class),
				mock(QueueMediaSources.class),
				mock(RenderersFactory.class),
				mock(ManagePlayableFileVolume.class));

		engine = new FuturePromise<>(playbackEngineBuilder.build(new Library())).get();
	}

	@Test
	public void thenAnExoPlayerEngineIsBuilt() {
		assertThat(engine).isInstanceOf(ExoPlayerPlayableFilePreparationSourceProvider.class);
	}
}
