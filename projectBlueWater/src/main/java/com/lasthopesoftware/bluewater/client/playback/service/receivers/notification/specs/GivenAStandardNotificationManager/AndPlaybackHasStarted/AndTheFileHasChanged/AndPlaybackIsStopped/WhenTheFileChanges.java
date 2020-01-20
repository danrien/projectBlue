package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.IntentFilter;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.resources.specs.ScopedLocalBroadcastManagerBuilder;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.InvocationTargetException;

import static com.lasthopesoftware.resources.notifications.specs.FakeNotificationCompatBuilder.newFakeBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhenTheFileChanges extends AndroidContext {

	private static final Notification secondNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	@Override
	public void before() throws InvocationTargetException, InstantiationException, IllegalAccessException {
		final NotificationCompat.Builder builder = mock(NotificationCompat.Builder.class);
		when(builder.build())
			.thenReturn(new Notification())
			.thenReturn(secondNotification);

		when(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(newFakeBuilder(new Notification()));

		when(notificationContentBuilder.promiseNowPlayingNotification(any(), anyBoolean()))
			.thenReturn(new Promise<>(builder));

		when(notificationContentBuilder.promiseNowPlayingNotification(argThat(a -> new ServiceFile(2).equals(a)), anyBoolean()))
			.thenReturn(new Promise<>(builder));

		final PlaybackNotificationRouter playbackNotificationRouter =
			new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
				new NotificationsController(
					service.getObject(),
					notificationManager),
				new PlaybackNotificationsConfiguration("",43),
				notificationContentBuilder,
				() -> new Promise<>(newFakeBuilder(new Notification()))));

		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManagerBuilder.newScopedBroadcastManager(RuntimeEnvironment.application);

		final LocalPlaybackBroadcaster localPlaybackBroadcaster =
			new LocalPlaybackBroadcaster(localBroadcastManager);

		localBroadcastManager
			.registerReceiver(
				playbackNotificationRouter,
				Stream.of(playbackNotificationRouter.registerForIntents())
					.reduce(new IntentFilter(), (intentFilter, action) -> {
						intentFilter.addAction(action);
						return intentFilter;
					}));

		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistStart,
			new LibraryId(1),
			new PositionedFile(1, new ServiceFile(1)));

		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistChange,
			new LibraryId(1),
			new PositionedFile(1, new ServiceFile(1)));

		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistStop,
			new LibraryId(1),
			new PositionedFile(1, new ServiceFile(1)));

		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistChange,
			new LibraryId(1),
			new PositionedFile(1, new ServiceFile(2)));
	}

	@Test
	public void thenTheServiceIsStartedInTheForegroundOnce() {
		verify(service.getObject(), times(1))
			.startForeground(eq(43), any());
	}

	@Test
	public void thenTheServiceDoesNotContinueInTheBackground() {
		verify(service.getObject()).stopForeground(true);
	}

	@Test
	public void thenTheNotificationIsNotSetToTheSecondNotification() {
		verify(notificationManager, never()).notify(43, secondNotification);
	}
}
