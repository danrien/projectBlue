package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.BuildNowPlayingNotificationContent;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationBroadcaster;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenPlaybackIsPaused {
	private static final Notification pausedNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	private static final CreateAndHold<Object> testSetup = new AbstractSynchronousLazy<Object>() {
		@Override
		protected Object create() throws Throwable {

			when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
				.thenReturn(new Promise<>(new Notification()));

			when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), false))
				.thenReturn(new Promise<>(pausedNotification));

			final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
				new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration(43),
					notificationContentBuilder);

			final LocalPlaybackBroadcaster localPlaybackBroadcaster =
				new LocalPlaybackBroadcaster(RuntimeEnvironment.application);

			LocalBroadcastManager.getInstance(RuntimeEnvironment.application)
				.registerReceiver(
					playbackNotificationBroadcaster,
					Stream.of(playbackNotificationBroadcaster.registerForIntents())
						.reduce(new IntentFilter(), (intentFilter, action) -> {
							intentFilter.addAction(action);
							return intentFilter;
						}));

			localPlaybackBroadcaster.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistPause,
				1,
				new PositionedFile(1, new ServiceFile(1)));

			return new Object();
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceContinuesInTheBackground() {
		verify(service.getObject()).stopForeground(false);
	}

	@Test
	public void thenTheNotificationIsNeverSet() {
		verify(notificationManager, never()).notify(43, pausedNotification);
	}
}
