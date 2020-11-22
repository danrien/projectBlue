package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.lasthopesoftware.specs.AndroidContextRunner;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidContextRunner.class)
public class WhenTheFileChanges {

	private static final ControlNotifications notificationController = mock(ControlNotifications.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	public void before() {
		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(new Notification()));

		when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(new Notification())));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				notificationController,
				new NotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(new Notification())));

		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
	}

	@Test
	public void thenTheServiceHasNotStarted() {
		verify(notificationController, never()).notifyForeground(any(), anyInt());
	}

	@Test
	public void thenTheNotificationHasNotBeenBroadcast() {
		verify(notificationController, never()).notifyBackground(any(), anyInt());
	}
}
