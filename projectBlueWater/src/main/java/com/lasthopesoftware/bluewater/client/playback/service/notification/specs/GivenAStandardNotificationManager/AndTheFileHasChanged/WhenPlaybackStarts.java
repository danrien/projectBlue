package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndTheFileHasChanged;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenPlaybackStarts extends AndroidContext {

	private static final Notification startedNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
			.thenReturn(new Promise<>(newFakeBuilder(startedNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				service.getObject(),
				notificationManager,
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder);

		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
		playbackNotificationBroadcaster.notifyPlaying();
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(43, startedNotification);
	}
}
