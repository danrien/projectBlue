package com.lasthopesoftware.bluewater.client.playback.service.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification firstNotification = new Notification();
	private static final Notification secondNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(1).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(new Notification())));

		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(2).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(secondNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				new NotificationsController(
					service.getObject(),
					notificationManager),
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(firstNotification)));

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
		playbackNotificationBroadcaster.notifyPaused();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(2));
		playbackNotificationBroadcaster.notifyPlaying();
	}

	@Test
	public void thenTheServiceIsStartedWhenPlaybackStarts() {
		verify(service.getObject(), times(1))
			.startForeground(43, firstNotification);
	}

	@Test
	public void thenTheServiceContinuesInTheBackground() {
		verify(service.getObject()).stopForeground(false);
	}

	@Test
	public void thenTheNotificationIsSetToThePausedNotification() {
		verify(notificationManager).notify(43, secondNotification);
	}

	@Test
	public void thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify(service.getObject(), times(1))
			.startForeground(43, secondNotification);
	}
}
