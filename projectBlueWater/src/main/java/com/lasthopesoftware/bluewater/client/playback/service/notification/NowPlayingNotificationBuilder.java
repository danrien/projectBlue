package com.lasthopesoftware.bluewater.client.playback.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.Map;

public class NowPlayingNotificationBuilder
implements
	BuildNowPlayingNotificationContent {

	private final Context context;
	private final IConnectionProvider connectionProvider;
	private final MediaSessionCompat mediaSessionCompat;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private final ImageProvider imageProvider;
	private final PlaybackNotificationsConfiguration configuration;

	private ViewStructure viewStructure;

	public NowPlayingNotificationBuilder(Context context, IConnectionProvider connectionProvider, MediaSessionCompat mediaSessionCompat, CachedFilePropertiesProvider cachedFilePropertiesProvider, ImageProvider imageProvider, PlaybackNotificationsConfiguration configuration) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.mediaSessionCompat = mediaSessionCompat;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		this.imageProvider = imageProvider;
		this.configuration = configuration;
	}

	@Override
	public synchronized Promise<Notification> promiseNowPlayingNotification(ServiceFile serviceFile, boolean isPlaying) {
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey());

		if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
			viewStructure.release();
			viewStructure = null;
		}

		if (viewStructure == null)
			viewStructure = new ViewStructure(urlKeyHolder, serviceFile);

		if (viewStructure.promisedNowPlayingImage == null)
			viewStructure.promisedNowPlayingImage = imageProvider.promiseFileBitmap(serviceFile);

		if (viewStructure.promisedFileProperties == null)
			viewStructure.promisedFileProperties = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

		return viewStructure.promisedFileProperties
			.eventually(fileProperties -> {
				final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
				final String name = fileProperties.get(FilePropertiesProvider.NAME);

				final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, configuration.getNotificationChannel());
				builder
					.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
						.setCancelButtonIntent(PlaybackService.pendingKillService(context))
						.setMediaSession(mediaSessionCompat.getSessionToken())
						.setShowActionsInCompactView(1))
					.setOngoing(isPlaying)
					.setColor(ContextCompat.getColor(context, R.color.clearstream_dark))
					.setContentIntent(buildNowPlayingActivityIntent())
					.setDeleteIntent(PlaybackService.pendingKillService(context))
					.setShowWhen(false)
					.setSmallIcon(R.drawable.clearstream_logo_dark)
					.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
					.setContentTitle(name)
					.setContentText(artist)
					.addAction(new NotificationCompat.Action(
						R.drawable.av_rewind,
						context.getString(R.string.btn_previous),
						PlaybackService.pendingPreviousIntent(context)))
					.addAction(isPlaying
						? new NotificationCompat.Action(
						R.drawable.av_pause,
						context.getString(R.string.btn_pause),
						PlaybackService.pendingPauseIntent(context))
						: new NotificationCompat.Action(
						R.drawable.av_play,
						context.getString(R.string.btn_play),
						PlaybackService.pendingPlayingIntent(context)))
					.addAction(new NotificationCompat.Action(
						R.drawable.av_fast_forward,
						context.getString(R.string.btn_next),
						PlaybackService.pendingNextIntent(context)));

				if (!viewStructure.urlKeyHolder.equals(urlKeyHolder))
					return new Promise<>(builder.build());

				return viewStructure
					.promisedNowPlayingImage
					.then(
						bitmap -> {
							if (bitmap != null)
								builder.setLargeIcon(bitmap);

							return builder.build();
						},
						e -> builder.build());
			});
	}

	private PendingIntent buildNowPlayingActivityIntent() {
		// Set the notification area
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, 0, viewIntent, 0);
	}

	private static class ViewStructure {
		final UrlKeyHolder<Integer> urlKeyHolder;
		final ServiceFile serviceFile;
		Promise<Map<String, String>> promisedFileProperties;
		Promise<Bitmap> promisedNowPlayingImage;

		ViewStructure(UrlKeyHolder<Integer> urlKeyHolder, ServiceFile serviceFile) {
			this.urlKeyHolder = urlKeyHolder;
			this.serviceFile = serviceFile;
		}

		void release() {
			if (promisedNowPlayingImage == null) return;

			promisedNowPlayingImage
				.then(bitmap -> {
					if (bitmap != null)
						bitmap.recycle();

					return null;
				});

			promisedNowPlayingImage.cancel();
		}
	}
}
