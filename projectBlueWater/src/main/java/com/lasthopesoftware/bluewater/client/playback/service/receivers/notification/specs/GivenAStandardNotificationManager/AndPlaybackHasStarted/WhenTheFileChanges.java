package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.specs.GivenAStandardNotificationManager.AndPlaybackHasStarted;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class WhenTheFileChanges {

	private static final Notification startedNotification = new Notification();
	private static final CreateAndHold<Service> service = new Lazy<>(() -> spy(Robolectric.buildService(PlaybackService.class).get()));
	private static final NotificationManager notificationManager = mock(NotificationManager.class);
	private static final BuildNowPlayingNotificationContent notificationContentBuilder = mock(BuildNowPlayingNotificationContent.class);

	private static final CreateAndHold<Object> testSetup = new AbstractSynchronousLazy<Object>() {
		@Override
		protected Object create() throws Throwable {

			when(notificationContentBuilder.promiseNowPlayingNotification(new ServiceFile(1), true))
				.thenReturn(new Promise<>(startedNotification));

			final PlaybackNotificationBroadcaster playbackNotificationBroadcaster =
				new PlaybackNotificationBroadcaster(
					service.getObject(),
					notificationManager,
					new PlaybackNotificationsConfiguration(43),
					notificationContentBuilder);

			playbackNotificationBroadcaster
				.onReceive(
					RuntimeEnvironment.application,
					new Intent(PlaylistEvents.onPlaylistStart));

			{
				final Intent playlistChangeIntent = new Intent(PlaylistEvents.onPlaylistChange);
				playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1);
				playbackNotificationBroadcaster
					.onReceive(
						RuntimeEnvironment.application,
						playlistChangeIntent);
			}

			return new Object();
		}
	};

	@Before
	public void context() {
		testSetup.getObject();
	}

	@Test
	public void thenTheServiceIsStartedInTheForeground() {
		verify(service.getObject()).startForeground(43, startedNotification);
	}
}