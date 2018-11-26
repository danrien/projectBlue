package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.*;
import com.lasthopesoftware.bluewater.client.playback.file.volume.IPlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.namehillsoftware.handoff.promises.Promise;
import io.reactivex.Observable;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenStartingPlayback {

	private List<PositionedPlayingFile> positionedPlayingFiles;

	@Before
	public void before() {
		PlayingFile mockPlayingFile = mock(PlayingFile.class);
		when(mockPlayingFile.promisePlayedFile()).thenReturn(new ProgressingPromise<Duration, PlayedFile>() {
			{
				resolve(mock(PlayedFile.class));
			}
			@Override
			public Duration getProgress() {
				return Duration.ZERO;
			}
		});
		PlayableFile playbackHandler = mock(PlayableFile.class);
		when(playbackHandler.promisePlayback()).thenReturn(new Promise<>(mockPlayingFile));

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new EmptyFileVolumeManager(),
				new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);

		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(null);

		Observable.create(new PlaylistPlayer(preparedPlaybackFileQueue, mock(IPlaybackHandlerVolumeControllerFactory.class), 0))
			.toList().subscribe(positionedPlayingFiles -> this.positionedPlayingFiles = positionedPlayingFiles);
	}

	@Test
	public void thenThePlaybackCountIsCorrect() {
		assertThat(this.positionedPlayingFiles.size()).isEqualTo(5);
	}
}
