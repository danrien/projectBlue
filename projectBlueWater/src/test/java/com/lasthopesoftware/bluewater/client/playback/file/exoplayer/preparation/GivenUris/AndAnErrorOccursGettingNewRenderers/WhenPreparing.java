package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAnErrorOccursGettingNewRenderers;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenPreparing {

	private static Throwable exception;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException {
		final LoadControl loadControl = mock(LoadControl.class);
		when(loadControl.getAllocator()).thenReturn(new DefaultAllocator(true, 1024));

		final ExoPlayerPlaybackPreparer preparer = new ExoPlayerPlaybackPreparer(
			mock(Context.class),
			uri -> mock(BaseMediaSource.class),
			loadControl,
			() -> new Promise<>(new Exception("Oops")),
			mock(Handler.class),
			(sf) -> new Promise<>(mock(Uri.class)));

		final Promise<PreparedPlayableFile> promisedPreparedFile =
			preparer.promisePreparedPlaybackFile(
				new ServiceFile(1),
				0);

		try {
			new FuturePromise<>(promisedPreparedFile).get(1, TimeUnit.SECONDS);
		} catch (ExecutionException ex) {
			exception = ex.getCause();
		}
	}

	@Test
	public void thenAnExceptionIsThrown() {
		assertThat(exception.getMessage()).isEqualTo("Oops");
	}
}
