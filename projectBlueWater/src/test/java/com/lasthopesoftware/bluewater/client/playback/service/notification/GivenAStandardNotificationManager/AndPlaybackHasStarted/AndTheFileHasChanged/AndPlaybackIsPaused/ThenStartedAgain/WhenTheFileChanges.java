package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain;

import android.app.Notification;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification loadingNotification = new Notification();
	private static final Notification startingNotification = new Notification();
	private static final Notification firstNotification = new Notification();
	private static final Notification secondNotification = new Notification();
	private static final ControlNotifications notificationController = mock(ControlNotifications.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() {
		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(loadingNotification));

		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(1).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(firstNotification)));

		when(notificationContentBuilder.promiseNowPlayingNotification(
			argThat(arg -> new ServiceFile(2).equals(arg)),
			anyBoolean()))
			.thenReturn(new Promise<>(newFakeBuilder(secondNotification)));

		final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
			new PlaybackNotificationBroadcaster(
				notificationController,
				new NotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(startingNotification)));

		playbackNotificationBroadcaster.notifyPlaying();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(1));
		playbackNotificationBroadcaster.notifyPaused();
		playbackNotificationBroadcaster.notifyPlayingFileChanged(new ServiceFile(2));
		playbackNotificationBroadcaster.notifyPlaying();
	}

	@Test
	public void thenTheLoadingNotificationIsShownManyTimes() {
		verify(notificationController, times(2)).notifyForeground(loadingNotification, 43);
		verify(notificationController, times(1)).notifyBackground(loadingNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedOnTheFirstServiceItem() {
		verify(notificationController, times(1))
			.notifyForeground(startingNotification, 43);
	}

	@Test
	public void thenTheNotificationIsSetToThePausedNotification() {
		verify(notificationController).notifyBackground(secondNotification, 43);
	}

	@Test
	public void thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify(notificationController, times(1))
			.notifyForeground(secondNotification, 43);
	}
}
