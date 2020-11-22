package com.lasthopesoftware.bluewater.client.playback.service;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.AudioCacheConfiguration;
import com.lasthopesoftware.bluewater.client.browsing.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.CachedFilesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideLibraryFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.LibrarySelectionKey;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.HttpDataSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.MediaSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.QueueProviders;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.IPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaybackStartedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.exceptions.BreakConnection;
import com.lasthopesoftware.bluewater.client.playback.service.exceptions.ConnectionCircuitTracker;
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.MediaStyleNotificationSetup;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.NowPlayingNotificationBuilder;
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.PlaybackStartingNotificationBuilder;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.AudioBecomingNoisyReceiver;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.MediaSessionCallbackReceiver;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.RemoteControlReceiver;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.RemoteControlProxy;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.MediaSessionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.RemoteControlClientBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings;
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator;
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator;
import com.lasthopesoftware.resources.notifications.NotificationBuilderProducer;
import com.lasthopesoftware.resources.notifications.control.ControlNotifications;
import com.lasthopesoftware.resources.notifications.control.NotificationsController;
import com.lasthopesoftware.resources.notifications.notificationchannel.ActivateChannel;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import com.namehillsoftware.projectblue.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.namehillsoftware.projectblue.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.namehillsoftware.projectblue.playback.engine.preparation.PreparationException;
import com.namehillsoftware.projectblue.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.namehillsoftware.projectblue.playback.file.EmptyPlaybackHandler;
import com.namehillsoftware.projectblue.playback.file.PlayedFile;
import com.namehillsoftware.projectblue.playback.file.PlayingFile;
import com.namehillsoftware.projectblue.playback.file.PositionedFile;
import com.namehillsoftware.projectblue.playback.file.PositionedPlayingFile;
import com.namehillsoftware.projectblue.playback.file.volume.MaxFileVolumeProvider;
import com.namehillsoftware.projectblue.playback.file.volume.preparation.MaxFileVolumePreparationProvider;
import com.namehillsoftware.projectblue.playback.volume.PlaylistVolumeManager;
import com.namehillsoftware.projectblue.shared.GenericBinder;
import com.namehillsoftware.projectblue.shared.MagicPropertyBuilder;
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.projectblue.shared.promises.extensions.ProgressedPromise;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.internal.schedulers.SingleScheduler;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

public class PlaybackService
extends Service
implements OnAudioFocusChangeListener
{
	private static final Logger logger = LoggerFactory.getLogger(PlaybackService.class);

	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, PlaybackService.class);
		newIntent.setAction(action);
		return newIntent;
	}

	public static void launchMusicService(final Context context, String serializedFileList) {
		launchMusicService(context, 0, serializedFileList);
	}

	public static void launchMusicService(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, Action.launchMusicService);
		svcIntent.putExtra(Action.Bag.playlistPosition, filePos);
		svcIntent.putExtra(Action.Bag.filePlaylist, serializedFileList);
		safelyStartService(context, svcIntent);
	}

	public static void seekTo(final Context context, int filePos) {
		seekTo(context, filePos, 0);
	}

	public static void seekTo(final Context context, int filePos, int fileProgress) {
		final Intent svcIntent = getNewSelfIntent(context, Action.seekTo);
		svcIntent.putExtra(Action.Bag.playlistPosition, filePos);
		svcIntent.putExtra(Action.Bag.startPos, fileProgress);
		safelyStartService(context, svcIntent);
	}

	public static void play(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.play));
	}

	public static PendingIntent pendingPlayingIntent(final Context context) {
		return PendingIntent.getService(
			context,
			0,
			getNewSelfIntent(
				context,
				PlaybackService.Action.play),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static void pause(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.pause));
	}

	public static PendingIntent pendingPauseIntent(final Context context) {
		return PendingIntent.getService(
			context,
			0,
			getNewSelfIntent(
				context,
				Action.pause),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static void togglePlayPause(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.togglePlayPause));
	}

	public static void next(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.next));
	}

	public static PendingIntent pendingNextIntent(final Context context) {
		return PendingIntent.getService(
			context,
			0,
			getNewSelfIntent(
				context,
				Action.next),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static void previous(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.previous));
	}

	public static PendingIntent pendingPreviousIntent(final Context context) {
		return PendingIntent.getService(
			context,
			0,
			getNewSelfIntent(
				context,
				Action.previous),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public static void setRepeating(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.repeating));
	}

	public static void setCompleting(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.completing));
	}

	public static void addFileToPlaylist(final Context context, int fileKey) {
		final Intent intent = getNewSelfIntent(context, Action.addFileToPlaylist);
		intent.putExtra(Action.Bag.playlistPosition, fileKey);
		safelyStartService(context, intent);
	}

	public static void removeFileAtPositionFromPlaylist(final Context context, int filePosition) {
		final Intent intent = getNewSelfIntent(context, Action.removeFileAtPositionFromPlaylist);
		intent.putExtra(Action.Bag.filePosition, filePosition);
		safelyStartService(context, intent);
	}

	public static void killService(final Context context) {
		safelyStartService(context, getNewSelfIntent(context, Action.killMusicService));
	}

	public static PendingIntent pendingKillService(final Context context) {
		return PendingIntent.getService(
			context,
			0,
			getNewSelfIntent(
				context,
				Action.killMusicService),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private static void safelyStartService(Context context, Intent intent) {
		try {
			context.startService(intent);
		} catch (IllegalStateException e) {
			logger.warn("An illegal state exception occurred while trying to start the service", e);
		} catch (SecurityException e) {
			logger.warn("A security exception occurred while trying to start the service", e);
		}
	}

	/* End streamer intent helpers */

	private static final String mediaSessionTag = MagicPropertyBuilder.buildMagicPropertyName(PlaybackService.class, "mediaSessionTag");

	private static final int playingNotificationId = 42;
	private static final int startingNotificationId = 53;
	private static final int connectingNotificationId = 70;

	private static final CreateAndHold<Scheduler> lazyObservationScheduler = new AbstractSynchronousLazy<Scheduler>() {
		@Override
		protected Scheduler create() {
			return new SingleScheduler(
				new RxThreadFactory(
					"Playback Observation",
					Thread.MIN_PRIORITY,
					false
				));
		}
	};

	private final CreateAndHold<NotificationManager> notificationManagerLazy = new Lazy<>(() -> (NotificationManager) getSystemService(NOTIFICATION_SERVICE));
	private final CreateAndHold<AudioManager> audioManagerLazy = new Lazy<>(() -> (AudioManager)getSystemService(Context.AUDIO_SERVICE));
	private final CreateAndHold<LocalBroadcastManager> localBroadcastManagerLazy = new Lazy<>(() -> LocalBroadcastManager.getInstance(this));
	private final CreateAndHold<ComponentName> remoteControlReceiver = new Lazy<>(() -> new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));
	private final CreateAndHold<RemoteControlClient> remoteControlClient = new AbstractSynchronousLazy<RemoteControlClient>() {
		@Override
		protected RemoteControlClient create() {
			// build the PendingIntent for the remote control client
			final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			mediaButtonIntent.setComponent(remoteControlReceiver.getObject());
			final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(PlaybackService.this, 0, mediaButtonIntent, 0);
			// create and register the remote control client

			return new RemoteControlClient(mediaPendingIntent);
		}
	};
	private final CreateAndHold<MediaSessionCompat> lazyMediaSession =
		new AbstractSynchronousLazy<MediaSessionCompat>() {
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			protected MediaSessionCompat create() {
				final MediaSessionCompat newMediaSession = new MediaSessionCompat(
					PlaybackService.this,
					mediaSessionTag);

				newMediaSession.setCallback(new MediaSessionCallbackReceiver(PlaybackService.this));

				final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
				mediaButtonIntent.setComponent(remoteControlReceiver.getObject());
				final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(PlaybackService.this, 0, mediaButtonIntent, 0);
				newMediaSession.setMediaButtonReceiver(mediaPendingIntent);

				return newMediaSession;
			}
		};
	private final CreateAndHold<IPlaybackBroadcaster> lazyPlaybackBroadcaster = new Lazy<>(() -> new LocalPlaybackBroadcaster(localBroadcastManagerLazy.getObject()));
	private final CreateAndHold<ISelectedLibraryIdentifierProvider> lazyChosenLibraryIdentifierProvider = new Lazy<>(() -> new SelectedBrowserLibraryIdentifierProvider(this));
	private final CreateAndHold<PlaybackStartedBroadcaster> lazyPlaybackStartedBroadcaster = new Lazy<>(() -> new PlaybackStartedBroadcaster(localBroadcastManagerLazy.getObject()));
	private final CreateAndHold<LibraryRepository> lazyLibraryRepository = new Lazy<>(() -> new LibraryRepository(this));
	private final CreateAndHold<PlaylistVolumeManager> lazyPlaylistVolumeManager = new Lazy<>(() -> new PlaylistVolumeManager(1.0f));
	private final CreateAndHold<IVolumeLevelSettings> lazyVolumeLevelSettings = new Lazy<>(() -> new VolumeLevelSettings(this));
	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new Lazy<>(() -> new SharedChannelProperties(this));
	private final CreateAndHold<NotificationsConfiguration> lazyPlaybackNotificationsConfiguration = new AbstractSynchronousLazy<NotificationsConfiguration>() {
		@Override
		protected NotificationsConfiguration create() {
			final ActivateChannel notificationChannelActivator = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
				? new NotificationChannelActivator(notificationManagerLazy.getObject())
				: new NoOpChannelActivator();

			final String channelName = notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());

			return new NotificationsConfiguration(channelName, playingNotificationId);
		}
	};
	private final CreateAndHold<MediaStyleNotificationSetup> lazyMediaStyleNotificationSetup = new AbstractSynchronousLazy<MediaStyleNotificationSetup>() {
		@Override
		protected MediaStyleNotificationSetup create() {
			return new MediaStyleNotificationSetup(
				PlaybackService.this,
				new NotificationBuilderProducer(PlaybackService.this),
				lazyPlaybackNotificationsConfiguration.getObject(),
				lazyMediaSession.getObject());
		}
	};
	private final CreateAndHold<GetAllStoredFilesInLibrary> lazyAllStoredFilesInLibrary = new Lazy<>(() -> new StoredFilesCollection(this));

	private final CreateAndHold<Promise<HandlerThread>> extractorThread = new AbstractSynchronousLazy<Promise<HandlerThread>>() {
		@Override
		protected Promise<HandlerThread> create() {
			return HandlerThreadCreator.promiseNewHandlerThread(
				"Media Extracting thread",
				Process.THREAD_PRIORITY_AUDIO);
		}
	};

	private final CreateAndHold<Promise<Handler>> extractorHandler = new AbstractSynchronousLazy<Promise<Handler>>() {
		@Override
		protected Promise<Handler> create() {
			return extractorThread.getObject().then(h -> new Handler(h.getLooper()));
		}
	};

	private final CreateAndHold<PlaybackStartingNotificationBuilder> lazyPlaybackStartingNotificationBuilder = new AbstractSynchronousLazy<PlaybackStartingNotificationBuilder>() {
		@Override
		protected PlaybackStartingNotificationBuilder create() {
			return new PlaybackStartingNotificationBuilder(
				PlaybackService.this,
				new NotificationBuilderProducer(PlaybackService.this),
				lazyPlaybackNotificationsConfiguration.getObject(),
				lazyMediaSession.getObject());
		}
	};

	private final CreateAndHold<ISelectedBrowserLibraryProvider> lazySelectedLibraryProvider = new AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
		@Override
		protected ISelectedBrowserLibraryProvider create() {
			return new SelectedBrowserLibraryProvider(
				new SelectedBrowserLibraryIdentifierProvider(PlaybackService.this),
				new LibraryRepository(PlaybackService.this));
		}
	};

	private final CreateAndHold<ProvideLibraryFileProperties> lazyFileProperties = new Lazy<>(() -> new FilePropertiesProvider(
		LibraryConnectionProvider.Instance.get(this),
		FilePropertyCache.getInstance()));

	private final CreateAndHold<CachedFilePropertiesProvider> lazyCachedFileProperties = new Lazy<>(() -> new CachedFilePropertiesProvider(
		LibraryConnectionProvider.Instance.get(this),
		FilePropertyCache.getInstance(),
		lazyFileProperties.getObject()));

	private final CreateAndHold<AudioBecomingNoisyReceiver> lazyAudioBecomingNoisyReceiver = new Lazy<>(AudioBecomingNoisyReceiver::new);

	private final CreateAndHold<ControlNotifications> lazyNotificationController = new Lazy<>(() -> new NotificationsController(this, notificationManagerLazy.getObject()));

	private final CreateAndHold<BreakConnection> lazyDisconnectionTracker = new Lazy<>(ConnectionCircuitTracker::new);

	private boolean areListenersRegistered = false;

	private Promise<PlaybackEngine> playbackEnginePromise;
	private PlaybackEngine playbackEngine;
	private PreparedPlaybackQueueResourceManagement playbackQueues;
	private CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider;
	private PositionedPlayingFile positionedPlayingFile;
	private boolean isPlaying;
	private Disposable filePositionSubscription;
	private PlaylistPlaybackBootstrapper playlistPlaybackBootstrapper;
	private RemoteControlProxy remoteControlProxy;
	private PlaybackNotificationRouter playbackNotificationRouter;
	private NowPlayingNotificationBuilder nowPlayingNotificationBuilder;

	private PowerManager.WakeLock wakeLock = null;
	private SimpleCache cache;
	private int startId;

	private final CreateAndHold<ImmediateResponse<IConnectionProvider, Void>> connectionRegainedListener = new AbstractSynchronousLazy<ImmediateResponse<IConnectionProvider, Void>>() {
		@Override
		protected final ImmediateResponse<IConnectionProvider, Void> create() {
			return new VoidResponse<>((c) -> {
				if (playbackEngine == null) {
					stopSelf(startId);
					return;
				}

				playbackEngine.resume();
			});
		}
	};

	private final CreateAndHold<ImmediateResponse<Throwable, Void>> onPollingCancelledListener = new AbstractSynchronousLazy<ImmediateResponse<Throwable, Void>>() {
		@Override
		protected final ImmediateResponse<Throwable, Void> create() {
			return new VoidResponse<>((e) -> {
				if (e instanceof CancellationException) {
					unregisterListeners();
					stopSelf(startId);
				}
			});
		}
	};

	private final BroadcastReceiver onLibraryChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int chosenLibrary = intent.getIntExtra(LibrarySelectionKey.chosenLibraryKey, -1);
			if (chosenLibrary < 0) return;

			pausePlayback(true);
			stopSelf(startId);
		}
	};

	private final BroadcastReceiver onPlaybackEngineChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			pausePlayback(true);
			stopSelf(startId);
		}
	};

	private final BroadcastReceiver buildSessionReceiver  = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int buildStatus = intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1);
			handleBuildConnectionStatusChange(buildStatus);
		}
	};

	private final ImmediateResponse<Throwable, Void> UnhandledRejectionHandler = (e) -> {
		uncaughtExceptionHandler(e);
		return null;
	};

	public boolean isPlaying() {
		return isPlaying;
	}

	private static Notification buildFullNotification(Builder notificationBuilder) {
		return notificationBuilder
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.build();
	}

	private void stopNotificationIfNotPlaying() {
		if (!isPlaying)
			lazyNotificationController.getObject().removeNotification(playingNotificationId);
	}

	private Promise<Void> notifyStartingService() {
		return lazyPlaybackStartingNotificationBuilder.getObject()
			.promisePreparedPlaybackStartingNotification()
			.then(new VoidResponse<>(b -> lazyNotificationController.getObject().notifyForeground(
				b.build(), startingNotificationId)));
	}

	private void registerListeners() {
		audioManagerLazy.getObject().requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
		wakeLock.acquire();

		registerRemoteClientControl();

		registerReceiver(
			lazyAudioBecomingNoisyReceiver.getObject(),
			new IntentFilter(ACTION_AUDIO_BECOMING_NOISY));

		areListenersRegistered = true;
	}

	private void registerRemoteClientControl() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			lazyMediaSession.getObject().setActive(true);
			return;
		}

		audioManagerLazy.getObject().registerMediaButtonEventReceiver(remoteControlReceiver.getObject());
		audioManagerLazy.getObject().registerRemoteControlClient(remoteControlClient.getObject());
	}

	private void unregisterListeners() {
		audioManagerLazy.getObject().abandonAudioFocus(this);

		if (wakeLock != null) {
			if (wakeLock.isHeld()) wakeLock.release();
			wakeLock = null;
		}

		if (lazyAudioBecomingNoisyReceiver.isCreated())
			unregisterReceiver(lazyAudioBecomingNoisyReceiver.getObject());

		areListenersRegistered = false;
	}

	/* Begin Event Handlers */

	@Override
	public final void onCreate() {
		registerRemoteClientControl();

		localBroadcastManagerLazy.getObject()
			.registerReceiver(
				onPlaybackEngineChanged,
				new IntentFilter(PlaybackEngineTypeChangedBroadcaster.playbackEngineTypeChanged));

		localBroadcastManagerLazy.getObject()
			.registerReceiver(
				onLibraryChanged,
				new IntentFilter(BrowserLibrarySelection.libraryChosenEvent));
	}

	@Override
	public final int onStartCommand(final Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		this.startId = startId;

		if (intent == null || intent.getAction() == null) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		final String action = intent.getAction();
		if (Action.killMusicService.equals(action) || !Action.validActions.contains(action)) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		if (playbackEngine != null) {
			actOnIntent(intent).excuse(UnhandledRejectionHandler);
			return START_NOT_STICKY;
		}

		notifyStartingService()
			.eventually(v -> lazySelectedLibraryProvider.getObject().getBrowserLibrary())
			.eventually(this::initializePlaybackPlaylistStateManagerSerially)
			.eventually(m -> actOnIntent(intent))
			.must(() -> lazyNotificationController.getObject().removeNotification(startingNotificationId))
			.excuse(UnhandledRejectionHandler);

		return START_NOT_STICKY;
	}

	private Promise<?> actOnIntent(final Intent intent) {
		if (intent == null) return Promise.empty();

		String action = intent.getAction();
		if (action == null) return Promise.empty();

		if (action.equals(Action.launchMusicService)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return Promise.empty();

			final String playlistString = intent.getStringExtra(Action.Bag.filePlaylist);
			if (playlistString == null) return Promise.empty();

			return FileStringListUtilities
				.promiseParsedFileStringList(playlistString)
				.eventually(playlist -> {
					final Promise<?> promiseStartedPlaylist = playbackEngine.startPlaylist(
						playlist instanceof List
							? (List<ServiceFile>)playlist
							: new ArrayList<>(playlist),
						playlistPosition,
						0);

					NowPlayingActivity.startNowPlayingActivity(this);

					return promiseStartedPlaylist;
				})
				.then(new VoidResponse<>(playlist -> localBroadcastManagerLazy.getObject().sendBroadcast(new Intent(PlaylistEvents.onPlaylistChange))));
		}

		if (action.equals(Action.togglePlayPause))
			action = isPlaying ? Action.pause : Action.play;

		if (action.equals(Action.play)) {
			isPlaying = true;
			return playbackEngine.resume();
		}

		if (action.equals(Action.pause)) return pausePlayback(true);

		if (!Action.playbackStartingActions.contains(action))
			stopNotificationIfNotPlaying();

		if (action.equals(Action.repeating)) {
			playbackEngine.playRepeatedly();
			return Promise.empty();
		}

		if (action.equals(Action.completing)) {
			playbackEngine.playToCompletion();
			return Promise.empty();
		}

		if (action.equals(Action.seekTo)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return Promise.empty();

			final int filePosition = intent.getIntExtra(Action.Bag.startPos, -1);
			if (filePosition < 0) return Promise.empty();

			return playbackEngine
				.changePosition(playlistPosition, filePosition)
				.then(this::broadcastChangedFile);
		}

		if (action.equals(Action.previous)) {
			return playbackEngine
				.skipToPrevious()
				.then(this::broadcastChangedFile);
		}

		if (action.equals(Action.next)) {
			return playbackEngine
				.skipToNext()
				.then(this::broadcastChangedFile);
		}

		if (action.equals(Action.addFileToPlaylist)) {
			final int fileKey = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (fileKey < 0) return Promise.empty();

			return playbackEngine
				.addFile(new ServiceFile(fileKey))
				.then(new VoidResponse<>(playlist -> localBroadcastManagerLazy.getObject().sendBroadcast(new Intent(PlaylistEvents.onPlaylistChange))))
				.eventually(LoopedInPromise.response(library -> {
					Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();
					return null;
				}, this));
		}

		if (action.equals(Action.removeFileAtPositionFromPlaylist)) {
			final int filePosition = intent.getIntExtra(Action.Bag.filePosition, -1);
			if (filePosition < -1) return Promise.empty();

			return playbackEngine
				.removeFileAtPosition(filePosition)
				.then(new VoidResponse<>(playlist -> localBroadcastManagerLazy.getObject().sendBroadcast(new Intent(PlaylistEvents.onPlaylistChange))));
		}

		return Promise.empty();
	}

	private synchronized Promise<PlaybackEngine> initializePlaybackPlaylistStateManagerSerially(Library library) {
		return playbackEnginePromise =
			playbackEnginePromise != null
				? playbackEnginePromise.eventually(
					e -> initializePlaybackEngine(library),
					e -> initializePlaybackEngine(library))
				: initializePlaybackEngine(library);
	}

	private Promise<PlaybackEngine> initializePlaybackEngine(Library library) {
		if (playbackEngine != null)
			playbackEngine.close();

		return getSessionConnection().eventually(connectionProvider -> {
			if (connectionProvider == null)
				throw new PlaybackEngineInitializationException("connectionProvider was null!");

			return extractorHandler.getObject().eventually(handler -> {
				cachedSessionFilePropertiesProvider = new CachedSessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance(),
					new SessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance()));
				if (remoteControlProxy != null)
					localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy);

				final ImageProvider imageProvider = new ImageProvider(
					new StaticLibraryIdentifierProvider(lazyChosenLibraryIdentifierProvider.getObject()),
					MemoryCachedImageAccess.getInstance(this));

				remoteControlProxy =
					new RemoteControlProxy(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
						? new MediaSessionBroadcaster(
							this,
							cachedSessionFilePropertiesProvider,
							imageProvider,
							lazyMediaSession.getObject())
						: new RemoteControlClientBroadcaster(
							this,
							cachedSessionFilePropertiesProvider,
							imageProvider,
							remoteControlClient.getObject()));

				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						remoteControlProxy,
						buildRemoteControlProxyIntentFilter(remoteControlProxy));

				if (playbackNotificationRouter != null)
					localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter);

				if (nowPlayingNotificationBuilder != null)
					nowPlayingNotificationBuilder.close();

				playbackNotificationRouter =
					new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
						lazyNotificationController.getObject(),
						lazyPlaybackNotificationsConfiguration.getObject(),
						nowPlayingNotificationBuilder = new NowPlayingNotificationBuilder(
							this,
							lazyMediaStyleNotificationSetup.getObject(),
							connectionProvider,
							cachedSessionFilePropertiesProvider,
							imageProvider),
						lazyPlaybackStartingNotificationBuilder.getObject()));

				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						playbackNotificationRouter,
						buildNotificationRouterIntentFilter(playbackNotificationRouter));

				if (playlistPlaybackBootstrapper != null)
					playlistPlaybackBootstrapper.close();

				playlistPlaybackBootstrapper = new PlaylistPlaybackBootstrapper(
					lazyPlaylistVolumeManager.getObject());

				final StoredFileAccess storedFileAccess = new StoredFileAccess(
					this,
					lazyAllStoredFilesInLibrary.getObject());

				final ExternalStorageReadPermissionsArbitratorForOs arbitratorForOs =
					new ExternalStorageReadPermissionsArbitratorForOs(this);

				final AudioCacheConfiguration cacheConfiguration = new AudioCacheConfiguration(library);
				if (cache != null)
					cache.release();
				cache = new SimpleCache(
					new AndroidDiskCacheDirectoryProvider(this).getDiskCacheDirectory(cacheConfiguration),
					new LeastRecentlyUsedCacheEvictor(cacheConfiguration.getMaxSize()));

				final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(
					connectionProvider,
					new ServiceFileUriQueryParamsProvider());

				final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(
					library,
					new StoredFileUriProvider(
						lazySelectedLibraryProvider.getObject(),
						storedFileAccess,
						arbitratorForOs),
					new CachedAudioFileUriProvider(
						remoteFileUriProvider,
						new CachedFilesProvider(this, new AudioCacheConfiguration(library))),
					new MediaFileUriProvider(
						this,
						new MediaQueryCursorProvider(this, lazyCachedFileProperties.getObject()),
						arbitratorForOs,
						lazyChosenLibraryIdentifierProvider.getObject(),
						false),
					remoteFileUriProvider);

				final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
					new PreparedPlaybackQueueFeederBuilder(
						this,
						handler,
						new MediaSourceProvider(
							library,
							new HttpDataSourceFactoryProvider(this, connectionProvider, OkHttpFactory.getInstance()),
							cache),
						bestMatchUriProvider);

				final IPlayableFilePreparationSourceProvider preparationSourceProvider =
					new MaxFileVolumePreparationProvider(
						playbackEngineBuilder.build(library),
						new MaxFileVolumeProvider(
							lazyVolumeLevelSettings.getObject(),
							cachedSessionFilePropertiesProvider));

				if (playbackQueues != null)
					playbackQueues.close();

				playbackQueues = new PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider);

				return PlaybackEngine.createEngine(
						playbackQueues,
						QueueProviders.providers(),
						new NowPlayingRepository(
							new SpecificLibraryProvider(
                                    lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(),
								lazyLibraryRepository.getObject()),
							lazyLibraryRepository.getObject()),
						playlistPlaybackBootstrapper);
			});
		})
		.then(engine -> {
			playbackEngine = engine;
			engine
				.setOnPlaybackStarted(this::handlePlaybackStarted)
				.setOnPlayingFileChanged(this::changePositionedPlaybackFile)
				.setOnPlaylistError(this::uncaughtExceptionHandler)
				.setOnPlaybackCompleted(this::onPlaylistPlaybackComplete)
				.setOnPlaylistReset(this::broadcastResetPlaylist);
			return engine;
		});
	}

	private static IntentFilter buildRemoteControlProxyIntentFilter(RemoteControlProxy remoteControlProxy) {
		final IntentFilter intentFilter = new IntentFilter();
		for (final String action : remoteControlProxy.registerForIntents()) {
			intentFilter.addAction(action);
		}

		return intentFilter;
	}

	private static IntentFilter buildNotificationRouterIntentFilter(PlaybackNotificationRouter playbackNotificationRouter) {
		final IntentFilter intentFilter = new IntentFilter();
		for (final String action : playbackNotificationRouter.registerForIntents()) {
			intentFilter.addAction(action);
		}

		return intentFilter;
	}

	private Promise<IConnectionProvider> getSessionConnection() {
		localBroadcastManagerLazy.getObject()
			.registerReceiver(
				buildSessionReceiver,
				new IntentFilter(SessionConnection.buildSessionBroadcast));

		return SessionConnection.getInstance(this).promiseSessionConnection().must(() -> {
			localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver);
			lazyNotificationController.getObject().removeNotification(connectingNotificationId);
		});
	}

	private void handleBuildConnectionStatusChange(final int status) {
		final Builder notifyBuilder = new Builder(this, lazyPlaybackNotificationsConfiguration.getObject().getNotificationChannel());
		notifyBuilder
			.setOngoing(false)
			.setContentTitle(getText(R.string.title_svc_connecting_to_server));
		switch (status) {
			case BuildingSessionConnectionStatus.GettingLibrary:
				notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details));
				break;
			case BuildingSessionConnectionStatus.GettingLibraryFailed:
				Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_please_connect_to_valid_server), Toast.LENGTH_SHORT).show();
				return;
			case BuildingSessionConnectionStatus.SendingWakeSignal:
				notifyBuilder.setContentText(getString(R.string.sending_wake_signal));
				break;
			case BuildingSessionConnectionStatus.BuildingConnection:
				notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library));
				break;
			case BuildingSessionConnectionStatus.BuildingConnectionFailed:
				Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_error_connecting_try_again), Toast.LENGTH_SHORT).show();
				return;
			default:
				return;
		}
		lazyNotificationController.getObject().notifyForeground(
			buildFullNotification(notifyBuilder),
			connectingNotificationId);
	}

	private void handlePlaybackStarted() {
		isPlaying = true;
		lazyPlaybackStartedBroadcaster.getObject().broadcastPlaybackStarted();
	}

	private Promise<Void> pausePlayback(boolean isUserInterrupted) {
		isPlaying = false;

		if (isUserInterrupted && areListenersRegistered) unregisterListeners();

		if (playbackEngine == null) return Promise.empty();

		return playbackEngine.pause()
			.then(new VoidResponse<>(v -> {
				if (positionedPlayingFile != null)
					lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

				if (filePositionSubscription != null)
					filePositionSubscription.dispose();
			}));
	}

	private void uncaughtExceptionHandler(Throwable exception) {
		if (exception instanceof PlaybackEngineInitializationException) {
			handlePlaybackEngineInitializationException((PlaybackEngineInitializationException)exception);
			return;
		}

		if (exception instanceof PreparationException) {
			handlePreparationException((PreparationException)exception);
			return;
		}

		if (exception instanceof IOException) {
			handleIoException((IOException)exception);
			return;
		}

		if (exception instanceof ExoPlaybackException) {
			handleExoPlaybackException((ExoPlaybackException)exception);
		}

		if (exception instanceof PlaybackException) {
			handlePlaybackException((PlaybackException)exception);
			return;
		}

		logger.error("An unexpected error has occurred!", exception);
		lazyNotificationController.getObject().removeAllNotifications();
	}

	private void handlePlaybackEngineInitializationException(PlaybackEngineInitializationException exception) {
		logger.error("There was an error initializing the playback engine", exception);
		lazyNotificationController.getObject().removeAllNotifications();
	}

	private void handlePreparationException(PreparationException preparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.getPositionedFile().getServiceFile(), preparationException);
		uncaughtExceptionHandler(preparationException.getCause());
	}

	private void handlePlaybackException(PlaybackException exception) {
		final Throwable cause = exception.getCause();

		if (cause instanceof ExoPlaybackException) {
			handleExoPlaybackException((ExoPlaybackException)cause);
		}

		if (cause instanceof IllegalStateException) {
			logger.error("The player ended up in an illegal state - closing and restarting the player", exception);
			closeAndRestartPlaylistManager();

			return;
		}

		if (cause instanceof IOException) {
			handleIoException((IOException)exception.getCause());
			return;
		}

		if (cause != null) {
			uncaughtExceptionHandler(cause);
			return;
		}

		logger.error("An unexpected playback exception occurred", exception);
	}

	private void handleExoPlaybackException(ExoPlaybackException exception) {
		logger.error("An ExoPlaybackException occurred");

		final Throwable cause = exception.getCause();
		if (cause instanceof IllegalStateException) {
			logger.error("The ExoPlayer player ended up in an illegal state, closing and restarting the player", cause);
			closeAndRestartPlaylistManager();
			return;
		}

		if (cause != null)
			uncaughtExceptionHandler(exception.getCause());
	}

	private void handleIoException(IOException exception) {
		if (exception instanceof HttpDataSource.InvalidResponseCodeException) {
			final HttpDataSource.InvalidResponseCodeException i = (HttpDataSource.InvalidResponseCodeException)exception;
			if (i.responseCode == 416) {
				logger.warn("Received an error code of " + i.responseCode + ", will attempt restarting the player", i);
				closeAndRestartPlaylistManager();
				return;
			}
		}

		logger.error("An IO exception occurred during playback", exception);
		handleDisconnection();
	}

	private void handleDisconnection() {
		if (!lazyDisconnectionTracker.getObject().isConnectionPastThreshold()) return;

		PollConnectionService.pollSessionConnection(this, true)
			.then(connectionRegainedListener.getObject(), onPollingCancelledListener.getObject());
	}

	private void closeAndRestartPlaylistManager() {
		try {
			playbackEngine.close();
		} catch (Exception e) {
			uncaughtExceptionHandler(e);
			return;
		}

		lazyLibraryRepository.getObject()
			.getLibrary(lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId())
			.eventually(this::initializePlaybackPlaylistStateManagerSerially)
			.then(new VoidResponse<>(v -> {
				if (isPlaying)
					playbackEngine.resume();
			}))
			.excuse(UnhandledRejectionHandler);
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			if (lazyPlaylistVolumeManager.isCreated())
				lazyPlaylistVolumeManager.getObject().setVolume(1.0f);

			if (playbackEngine != null && !playbackEngine.isPlaying())
				playbackEngine.resume();

			return;
		}

		if (playbackEngine == null || !playbackEngine.isPlaying()) return;

	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_LOSS:
		        // Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		        // Lost focus but it will be regained... cannot release resources
		        pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (lazyPlaylistVolumeManager.isCreated())
					lazyPlaylistVolumeManager.getObject().setVolume(0.2f);
	    }
	}

	private void changePositionedPlaybackFile(PositionedPlayingFile positionedPlayingFile) {
		this.positionedPlayingFile = positionedPlayingFile;

		final PlayingFile playingFile = positionedPlayingFile.getPlayingFile();

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();

		if (playingFile instanceof EmptyPlaybackHandler) return;

		broadcastChangedFile(positionedPlayingFile.asPositionedFile());
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackStart, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

		final ProgressedPromise<Duration, PlayedFile> promisedPlayedFile = playingFile.promisePlayedFile();
		final Disposable localSubscription = filePositionSubscription =
			Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.getObject())
				.map(t -> promisedPlayedFile.getProgress())
				.distinctUntilChanged()
				.subscribe(new TrackPositionBroadcaster(
					localBroadcastManagerLazy.getObject(),
					playingFile));

		promisedPlayedFile.then(p -> {
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackComplete, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());
			localSubscription.dispose();
			return null;
		});

		if (!areListenersRegistered) registerListeners();
		registerRemoteClientControl();
	}

	private void broadcastResetPlaylist(PositionedFile positionedFile) {
		lazyPlaybackBroadcaster.getObject()
			.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistTrackChange,
                    lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(),
				positionedFile);
	}

	private Void broadcastChangedFile(PositionedFile positionedFile) {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistTrackChange, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedFile);
		return null;
	}

	private void onPlaylistPlaybackComplete() {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

		killService(this);
	}

	@Override
	public void onDestroy() {

		if (lazyNotificationController.isCreated())
			lazyNotificationController.getObject().removeAllNotifications();

		if (playlistPlaybackBootstrapper != null) {
			try {
				playlistPlaybackBootstrapper.close();
			} catch (IOException e) {
				logger.warn("There was an error closing the prepared playback bootstrapper", e);
			}
		}

		if (playbackEngine != null) {
			try {
				playbackEngine.close();
			} catch (Exception e) {
				logger.warn("There was an error closing the playback engine", e);
			}
		}

		if (playbackQueues != null) {
			try {
				playbackQueues.close();
			} catch (Exception e) {
				logger.warn("There was an error closing the prepared playback queue", e);
			}
		}

		if (areListenersRegistered) unregisterListeners();

		if (remoteControlReceiver.isCreated())
			audioManagerLazy.getObject().unregisterMediaButtonEventReceiver(remoteControlReceiver.getObject());

		if (remoteControlClient.isCreated())
			audioManagerLazy.getObject().unregisterRemoteControlClient(remoteControlClient.getObject());

		if (extractorThread.isCreated())
			extractorThread.getObject().then(HandlerThread::quitSafely);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && lazyMediaSession.isCreated()) {
			lazyMediaSession.getObject().setActive(false);
			lazyMediaSession.getObject().release();
		}

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();

		if (cache != null)
			cache.release();

		if (nowPlayingNotificationBuilder != null)
			nowPlayingNotificationBuilder.close();

		if (!localBroadcastManagerLazy.isCreated()) return;

		localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver);
		localBroadcastManagerLazy.getObject().unregisterReceiver(onLibraryChanged);
		localBroadcastManagerLazy.getObject().unregisterReceiver(onPlaybackEngineChanged);

		if (remoteControlProxy != null)
			localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy);

		if (playbackNotificationRouter != null)
			localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter);
	}

	/* End Event Handlers */

	/* Begin Binder Code */

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	private final Lazy<IBinder> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	/* End Binder Code */


	private static class Action {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Action.class);

		/* String constant actions */
		private static final String launchMusicService = magicPropertyBuilder.buildProperty("launchMusicService");
		private static final String play = magicPropertyBuilder.buildProperty("play");
		private static final String pause = magicPropertyBuilder.buildProperty("pause");
		private static final String togglePlayPause = magicPropertyBuilder.buildProperty("togglePlayPause");
		private static final String repeating = magicPropertyBuilder.buildProperty("repeating");
		private static final String completing = magicPropertyBuilder.buildProperty("completing");
		private static final String previous = magicPropertyBuilder.buildProperty("previous");
		private static final String next = magicPropertyBuilder.buildProperty("then");
		private static final String seekTo = magicPropertyBuilder.buildProperty("seekTo");
		private static final String addFileToPlaylist = magicPropertyBuilder.buildProperty("addFileToPlaylist");
		private static final String removeFileAtPositionFromPlaylist = magicPropertyBuilder.buildProperty("removeFileAtPositionFromPlaylist");
		private static final String killMusicService = magicPropertyBuilder.buildProperty("killMusicService");

		private static final Set<String> validActions = new HashSet<>(Arrays.asList(launchMusicService,
			play,
			pause,
			togglePlayPause,
			previous,
			next,
			seekTo,
			repeating,
			completing,
			addFileToPlaylist,
			removeFileAtPositionFromPlaylist));

		private static final Set<String> playbackStartingActions = new HashSet<>(Arrays.asList(
			launchMusicService,
			play,
			togglePlayPause));

		private static class Bag {
			private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(Bag.class);

			/* Bag constants */
			private static final String playlistPosition = magicPropertyBuilder.buildProperty("playlistPosition");
			private static final String filePlaylist = magicPropertyBuilder.buildProperty("filePlaylist");
			private static final String startPos = magicPropertyBuilder.buildProperty("startPos");
			private static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		}
	}
}
