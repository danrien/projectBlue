package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingFile.ThatIsThenPaused.AndThePlayerIdles;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenThePlayerWillNotPlayWhenReady {
	private static final Collection<Player.EventListener> eventListeners = new ArrayList<>();
	private static final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);;

	@BeforeClass
	public static void before() throws InterruptedException, ExecutionException {
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListeners.add(invocation.getArgument(0));
			return null;
		}).when(mockExoPlayer).addListener(any());

		ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		final Promise<PlayingFile> playbackPromise = exoPlayerPlaybackHandler.promisePlayback();

		playbackPromise
				.eventually(p -> {
					final Promise<PlayableFile> playableFilePromise = p.promisePause();
					Stream.of(eventListeners)
						.forEach(e -> e.onPlayerStateChanged(false, Player.STATE_IDLE));
					return playableFilePromise;
				});

		final Promise<PlayedFile> playedPromise =
				playbackPromise
						.eventually(PlayingFile::promisePlayedFile);

		try {
			new FuturePromise<>(playedPromise).get(1, TimeUnit.SECONDS);
		} catch (TimeoutException ignored) {
		}
	}

	@Test
	public void thenPlaybackIsNotRestarted() {
		verify(mockExoPlayer, times(1)).setPlayWhenReady(true);
	}
}
