package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification loadingNotification = new Notification();
	private static final Notification startedNotification = new Notification();
	private static final Notification nextNotification = new Notification();
	private static final ControlNotifications notificationController = mock(ControlNotifications.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(loadingNotification));

		when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(startedNotification)));

		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(2), true))
			.thenReturn(new Promise<>(newFakeBuilder(nextNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				notificationController,
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder);

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
	}

	@Test
	public void thenTheLoadingNotificationIsStarted() {
		verify(notificationController).notifyForeground(loadingNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(notificationController ).notifyForeground(startedNotification, 43);
	}
}
