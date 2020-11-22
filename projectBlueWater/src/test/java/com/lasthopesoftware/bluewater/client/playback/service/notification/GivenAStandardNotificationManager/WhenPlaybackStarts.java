package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager;

import android.app.Notification;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.namehillsoftware.projectblue.shared.AndroidContext;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WhenPlaybackStarts extends AndroidContext {
	private static final Notification startedNotification = new Notification();
	private static final ControlNotifications notificationController = mock(ControlNotifications.class);

	@Override
	public void before() {
		final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				notificationController,
				new NotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(FakeNotificationCompatBuilder.newFakeBuilder(startedNotification))));

		playbackNotificationRouter
			.onReceive(
				ApplicationProvider.getApplicationContext(),
				new Intent(PlaylistEvents.onPlaylistStart));
	}

	@Test
	public void thenAStartingNotificationIsSet() {
		verify(notificationController, times(1)).notifyForeground(startedNotification, 43);
	}
}
