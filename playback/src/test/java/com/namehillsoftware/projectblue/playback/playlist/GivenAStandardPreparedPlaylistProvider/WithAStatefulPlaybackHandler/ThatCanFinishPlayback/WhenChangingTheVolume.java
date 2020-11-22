package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue;
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.ResolvablePlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.playlist.PlaylistPlayer;

import org.junit.BeforeClass;
import org.junit.Test;

import io.reactivex.Observable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenChangingTheVolume {

	private static final NoTransformVolumeManager volumeManagerUnderTest = new NoTransformVolumeManager();

	@BeforeClass
	public static void before() {
		final ResolvablePlaybackHandler playbackHandler = new ResolvablePlaybackHandler();
		final ResolvablePlaybackHandler secondPlaybackHandler = new ResolvablePlaybackHandler();

		final Promise<PositionedPlayableFile> positionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				playbackHandler,
				new NoTransformVolumeManager(),
				new ServiceFile(1)));

		final Promise<PositionedPlayableFile> secondPositionedPlaybackHandlerContainer =
			new Promise<>(new PositionedPlayableFile(
				0,
				secondPlaybackHandler,
				volumeManagerUnderTest,
				new ServiceFile(1)));

		final PreparedPlayableFileQueue preparedPlaybackFileQueue = mock(PreparedPlayableFileQueue.class);
		when(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(secondPositionedPlaybackHandlerContainer);

		final IPlaylistPlayer playlistPlayback = new PlaylistPlayer(
				preparedPlaybackFileQueue,
				0);

		Observable.create(playlistPlayback).subscribe();

		playlistPlayback.setVolume(0.8f);

		playbackHandler.resolve();
	}

	@Test
	public void thenTheVolumeIsChanged() {
		assertThat(volumeManagerUnderTest.getVolume()).isEqualTo(0.8f);
	}
}
