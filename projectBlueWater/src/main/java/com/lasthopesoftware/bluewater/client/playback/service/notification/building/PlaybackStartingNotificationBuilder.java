package com.lasthopesoftware.bluewater.client.playback.service.notification.building;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
import com.namehillsoftware.handoff.promises.Promise;

public class PlaybackStartingNotificationBuilder {

	private final Context context;
	private final PlaybackNotificationsConfiguration configuration;

	public PlaybackStartingNotificationBuilder(Context context, PlaybackNotificationsConfiguration configuration) {
		this.context = context;
		this.configuration = configuration;
	}

	public Promise<NotificationCompat.Builder> promisePreparedPlaybackStartingNotification() {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, configuration.getNotificationChannel());
		builder
			.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
				.setCancelButtonIntent(PlaybackService.pendingKillService(context))
				.setMediaSession(configuration.getMediaSessionToken())
				.setShowCancelButton(true))
			.setOngoing(true)
			.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
			.setContentIntent(buildNowPlayingActivityIntent())
			.setShowWhen(false)
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(context.getString(R.string.lbl_starting_playback));

		return new Promise<>(builder);
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}
}
