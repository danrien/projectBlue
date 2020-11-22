package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationBroadcaster;

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class WhenRegisteringForActions {

	private static Collection<String> registeredIntents;

	@Before
	public void context() {
		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(mock(NotifyOfPlaybackEvents.class));

		registeredIntents = playbackNotificationRouter.registerForIntents();
	}

	@Test
	public void thenTheRegisteredActionsAreCorrect() {
		assertThat(registeredIntents).isSubsetOf(
			PlaylistEvents.onPlaylistTrackChange,
			PlaylistEvents.onPlaylistPause,
			PlaylistEvents.onPlaylistStart,
			PlaylistEvents.onPlaylistStop);
	}
}
