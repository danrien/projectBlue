package com.lasthopesoftware.bluewater.client.playback.service;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.*;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;
import com.annimon.stream.Stream;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.AudioCacheConfiguration;
import com.lasthopesoftware.bluewater.client.library.items.media.audio.uri.CachedAudioFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.access.CachedFilesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.RemoteFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.StartAndClosePlayback;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer.ExoPlayerCreator;
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.exoplayer.ExoPlayerPlaybackBootstrapper;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.AudioRenderingEventListener;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.MetadataOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.events.TextOutputLogger;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.MediaSourceQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparationException;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueFeederBuilder;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup;
import com.lasthopesoftware.bluewater.client.playback.file.*;
import com.lasthopesoftware.bluewater.client.playback.file.error.PlaybackException;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ExtractorMediaSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.HttpDataSourceFactoryProvider;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.QueueProviders;
import com.lasthopesoftware.bluewater.client.playback.file.volume.MaxFileVolumeProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerVolumeControllerFactory;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.*;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationsConfiguration;
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
import com.lasthopesoftware.bluewater.client.playback.volume.AudioTrackVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.LibrarySelectionKey;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.GetAllStoredFilesInLibrary;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.settings.volumeleveling.IVolumeLevelSettings;
import com.lasthopesoftware.bluewater.settings.volumeleveling.VolumeLevelSettings;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise;
import com.lasthopesoftware.compilation.DebugFlag;
import com.lasthopesoftware.resources.loopers.HandlerThreadCreator;
import com.lasthopesoftware.resources.notifications.NotificationBuilderProducer;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.RxThreadFactory;
import io.reactivex.internal.schedulers.SingleScheduler;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

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

	private static final String wifiLockSvcName =  MagicPropertyBuilder.buildMagicPropertyName(PlaybackService.class, "wifiLockSvcName");
	private static final String mediaSessionTag = MagicPropertyBuilder.buildMagicPropertyName(PlaybackService.class, "mediaSessionTag");

	private static final int notificationId = 42;
	private static int startId;

	private static final int maxErrors = 3;
	private static final int errorCountResetDuration = 1000;

	private static final CreateAndHold<TrackSelector> trackSelector = new Lazy<>(DefaultTrackSelector::new);
	private static final CreateAndHold<LoadControl> loadControl = new AbstractSynchronousLazy<LoadControl>() {
		@Override
		protected LoadControl create() {
			final DefaultLoadControl.Builder builder = new DefaultLoadControl.Builder();
			builder.setBufferDurationsMs(
				DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
				(int) Minutes.minutes(5).toStandardDuration().getMillis(),
				DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
				DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);

			return builder.createDefaultLoadControl();
		}
	};

	private static final CreateAndHold<TextOutputLogger> lazyTextOutputLogger = new Lazy<>(TextOutputLogger::new);
	private static final CreateAndHold<MetadataOutputLogger> lazyMetadataOutputLogger = new Lazy<>(MetadataOutputLogger::new);

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

				newMediaSession.setFlags(
					MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
					MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

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
	private final CreateAndHold<PlaybackStartedBroadcaster> lazyPlaybackStartedBroadcaster = new Lazy<>(() -> new PlaybackStartedBroadcaster(lazyChosenLibraryIdentifierProvider.getObject(), lazyPlaybackBroadcaster.getObject()));
	private final CreateAndHold<LibraryRepository> lazyLibraryRepository = new Lazy<>(() -> new LibraryRepository(this));
	private final CreateAndHold<PlaylistVolumeManager> lazyPlaylistVolumeManager = new Lazy<>(() -> new PlaylistVolumeManager(1.0f));
	private final CreateAndHold<IVolumeLevelSettings> lazyVolumeLevelSettings = new Lazy<>(() -> new VolumeLevelSettings(this));
	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new Lazy<>(() -> new SharedChannelProperties(this));
	private final CreateAndHold<PlaybackNotificationsConfiguration> lazyPlaybackNotificationsConfiguration = new AbstractSynchronousLazy<PlaybackNotificationsConfiguration>() {
		@Override
		protected PlaybackNotificationsConfiguration create() {
			final NotificationChannelActivator notificationChannelActivator = new NotificationChannelActivator(notificationManagerLazy.getObject());

			final String channelName = notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject());
			
			return new PlaybackNotificationsConfiguration(channelName, notificationId);
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

	private final CreateAndHold<AudioBecomingNoisyReceiver> lazyAudioBecomingNoisyReceiver = new Lazy<>(AudioBecomingNoisyReceiver::new);

	private final CreateAndHold<RenderersFactory> lazyRenderersFactory = new Lazy<>(() -> new DefaultRenderersFactory(this));

	private int numberOfErrors = 0;
	private long lastErrorTime = 0;

	private boolean areListenersRegistered = false;
	private boolean isNotificationForeground = false;

	private Promise<PlaybackEngine> playbackEnginePromise;
	private PlaybackEngine playbackEngine;
	private PreparedPlaybackQueueResourceManagement playbackQueues;
	private CachedFilePropertiesProvider cachedFilePropertiesProvider;
	private PositionedPlayingFile positionedPlayingFile;
	private boolean isPlaying;
	private Disposable filePositionSubscription;
	private StartAndClosePlayback playlistPlaybackBootstrapper;
	private RemoteControlProxy remoteControlProxy;
	private PlaybackNotificationRouter playbackNotificationRouter;
	private NowPlayingNotificationBuilder nowPlayingNotificationBuilder;
	private PreparedPlaybackQueueResourceManagement preparedPlaybackQueueResourceManagement;
	private ExoPlayer exoPlayer;

	private WifiLock wifiLock = null;
	private PowerManager.WakeLock wakeLock = null;
	private SimpleCache cache;

	private Promise<IConnectionProvider> pollingSessionConnection;

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

	private void notifyBackground(Builder notificationBuilder) {
		if (isNotificationForeground)
			stopForeground(false);

		isNotificationForeground = false;

		notifyNotificationManager(notificationBuilder);
	}

	private void notifyForeground(Builder notificationBuilder) {
		if (isNotificationForeground) {
			notifyNotificationManager(notificationBuilder);
			return;
		}

		startForeground(notificationId, addNotificationAccoutrements(notificationBuilder).build());
		isNotificationForeground = true;
	}

	private void notifyNotificationManager(Builder notificationBuilder) {
		notificationManagerLazy.getObject().notify(notificationId, addNotificationAccoutrements(notificationBuilder).build());
	}

	private static Builder addNotificationAccoutrements(Builder notificationBuilder) {
		return notificationBuilder
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
	}

	private void stopNotificationIfNotPlaying() {
		if (!isPlaying)
			stopNotification();
	}

	private void stopNotification() {
		stopForeground(true);
		isNotificationForeground = false;
		notificationManagerLazy.getObject().cancel(notificationId);
	}

	private void notifyStartingService() {
		lazyPlaybackStartingNotificationBuilder.getObject()
			.promisePreparedPlaybackStartingNotification()
			.then(new VoidResponse<>(this::notifyForeground));
	}
	
	private void registerListeners() {
		audioManagerLazy.getObject().requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
				
		wifiLock = ((WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, wifiLockSvcName);
        wifiLock.acquire();

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
		
		// release the wifilock if we still have it
		if (wifiLock != null) {
			if (wifiLock.isHeld()) wifiLock.release();
			wifiLock = null;
		}

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
		PlaybackService.startId = startId;

		final String action = intent.getAction();
		if (Action.killMusicService.equals(action) || !Action.validActions.contains(action)) {
			stopSelf(startId);
			return START_NOT_STICKY;
		}

		if (playbackEngine != null) {
			actOnIntent(intent);
			return START_NOT_STICKY;
		}

		notifyStartingService();
		lazySelectedLibraryProvider.getObject()
			.getBrowserLibrary()
			.eventually(this::initializePlaybackPlaylistStateManagerSerially)
			.then(new VoidResponse<>(m -> actOnIntent(intent)))
			.excuse(UnhandledRejectionHandler);

		return START_NOT_STICKY;
	}

	private void actOnIntent(final Intent intent) {
		if (intent == null) {
			pausePlayback(true);
			return;
		}

		String action = intent.getAction();
		if (action == null) return;

		if (action.equals(Action.launchMusicService)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return;

			FileStringListUtilities
				.promiseParsedFileStringList(intent.getStringExtra(Action.Bag.filePlaylist))
				.then(playlist -> {
					playbackEngine.startPlaylist(playlist, playlistPosition, 0);
					NowPlayingActivity.startNowPlayingActivity(this);

					return null;
				});

			return;
		}

		if (action.equals(Action.togglePlayPause))
			action = isPlaying ? Action.pause : Action.play;

		if (action.equals(Action.play)) {
			isPlaying = true;
			playbackEngine.resume();

			return;
		}

		if (action.equals(Action.pause)) {
			pausePlayback(true);
			return;
		}

		if (!Action.playbackStartingActions.contains(action))
			stopNotificationIfNotPlaying();

		if (action.equals(Action.repeating)) {
			playbackEngine.playRepeatedly();
			return;
		}

		if (action.equals(Action.completing)) {
			playbackEngine.playToCompletion();
			return;
		}

		if (action.equals(Action.seekTo)) {
			final int playlistPosition = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (playlistPosition < 0) return;

			final int filePosition = intent.getIntExtra(Action.Bag.startPos, -1);
			if (filePosition < 0) return;

			playbackEngine
				.changePosition(playlistPosition, filePosition)
				.then(this::broadcastChangedFile)
				.excuse(UnhandledRejectionHandler);

			return;
		}

		if (action.equals(Action.previous)) {
			playbackEngine
				.skipToPrevious()
				.then(this::broadcastChangedFile)
				.excuse(UnhandledRejectionHandler);
			return;
		}

		if (action.equals(Action.next)) {
			playbackEngine
				.skipToNext()
				.then(this::broadcastChangedFile)
				.excuse(UnhandledRejectionHandler);
			return;
		}

		if (pollingSessionConnection != null && action.equals(Action.stopWaitingForConnection)) {
			pollingSessionConnection.cancel();
			return;
		}

		if (action.equals(Action.addFileToPlaylist)) {
			final int fileKey = intent.getIntExtra(Action.Bag.playlistPosition, -1);
			if (fileKey < 0) return;

			playbackEngine
				.addFile(new ServiceFile(fileKey))
				.eventually(LoopedInPromise.response(library -> {
					Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();
					return library;
				}, this))
				.excuse(UnhandledRejectionHandler);

			return;
		}

		if (action.equals(Action.removeFileAtPositionFromPlaylist)) {
			final int filePosition = intent.getIntExtra(Action.Bag.filePosition, -1);
			if (filePosition < -1) return;

			playbackEngine.removeFileAtPosition(filePosition).excuse(UnhandledRejectionHandler);
		}
	}

	private synchronized Promise<PlaybackEngine> initializePlaybackPlaylistStateManagerSerially(Library library) {
		return playbackEnginePromise =
			playbackEnginePromise != null
				? playbackEnginePromise.eventually(
					e -> initializePlaybackPlaylistStateManager(library),
					e -> initializePlaybackPlaylistStateManager(library))
				: initializePlaybackPlaylistStateManager(library);
	}

	private Promise<PlaybackEngine> initializePlaybackPlaylistStateManager(Library library) {
		if (playbackEngine != null)
			playbackEngine.close();

		localBroadcastManagerLazy.getObject()
			.registerReceiver(
				buildSessionReceiver,
				new IntentFilter(SessionConnection.buildSessionBroadcast));

		return SessionConnection.getInstance(this).promiseSessionConnection().eventually(connectionProvider -> {
			localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver);

			if (connectionProvider == null)
				throw new PlaybackEngineInitializationException("connectionProvider was null!");

			return extractorHandler.getObject().eventually(handler -> {
				cachedFilePropertiesProvider = new CachedFilePropertiesProvider(
					connectionProvider,
					FilePropertyCache.getInstance(),
					new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance(), ParsingScheduler.instance()));

				if (remoteControlProxy != null)
					localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy);

				final ImageProvider imageProvider = new ImageProvider(this, connectionProvider, new AndroidDiskCacheDirectoryProvider(this), cachedFilePropertiesProvider);
				remoteControlProxy =
					new RemoteControlProxy(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
						? new MediaSessionBroadcaster(
							this,
							cachedFilePropertiesProvider,
							imageProvider,
							lazyMediaSession.getObject())
						: new RemoteControlClientBroadcaster(
							this,
							cachedFilePropertiesProvider,
							imageProvider,
							remoteControlClient.getObject()));

				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						remoteControlProxy,
						Stream.of(remoteControlProxy.registerForIntents())
							.reduce(new IntentFilter(), (intentFilter, action) -> {
								intentFilter.addAction(action);
								return intentFilter;
							}));

				if (playbackNotificationRouter != null)
					localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter);

				if (nowPlayingNotificationBuilder != null)
					nowPlayingNotificationBuilder.close();

				playbackNotificationRouter =
					new PlaybackNotificationRouter(new PlaybackNotificationBroadcaster(
						this,
						notificationManagerLazy.getObject(),
						lazyPlaybackNotificationsConfiguration.getObject(),
						nowPlayingNotificationBuilder = new NowPlayingNotificationBuilder(
							this,
							lazyMediaStyleNotificationSetup.getObject(),
							connectionProvider,
							cachedFilePropertiesProvider,
							imageProvider)));

				localBroadcastManagerLazy
					.getObject()
					.registerReceiver(
						playbackNotificationRouter,
						Stream.of(playbackNotificationRouter.registerForIntents())
							.reduce(new IntentFilter(), (intentFilter, action) -> {
								intentFilter.addAction(action);
								return intentFilter;
							}));

				if (playlistPlaybackBootstrapper != null)
					playlistPlaybackBootstrapper.close();

				playlistPlaybackBootstrapper = new PlaylistPlaybackBootstrapper(
					lazyPlaylistVolumeManager.getObject(),
					new PlaybackHandlerVolumeControllerFactory(
						new MaxFileVolumeProvider(lazyVolumeLevelSettings.getObject(), cachedFilePropertiesProvider)));

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

				if (playlistPlaybackBootstrapper != null)
					playlistPlaybackBootstrapper.close();

				playlistPlaybackBootstrapper = new ExoPlayerPlaybackBootstrapper(new ExoPlayerCreator(handler, lazyRenderersFactory.getObject()));

				final ExtractorMediaSourceFactoryProvider extractorMediaSourceFactoryProvider = new ExtractorMediaSourceFactoryProvider(
					library,
					new HttpDataSourceFactoryProvider(
						this,
						connectionProvider,
						OkHttpFactory.getInstance()),
					cache);

				final Renderer[] renderers = lazyRenderersFactory.getObject().createRenderers(
					handler,
					null,
					DebugFlag.getInstance().isDebugCompilation() ? new AudioRenderingEventListener() : null,
					lazyTextOutputLogger.getObject(),
					lazyMetadataOutputLogger.getObject(),
					null);

				exoPlayer = ExoPlayerFactory.newInstance(
					renderers,
					trackSelector.getObject(),
					loadControl.getObject());

				final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(
					connectionProvider,
					new ServiceFileUriQueryParamsProvider());

				exoPlayer = ExoPlayerFactory.newInstance(
					renderers,
					trackSelector.getObject(),
					loadControl.getObject());

				final MediaSourceQueue mediaSourceQueue = new MediaSourceQueue();

				exoPlayer.prepare(mediaSourceQueue);
				final PreparedPlaybackQueueFeederBuilder playbackEngineBuilder =
					new PreparedPlaybackQueueFeederBuilder(
						new SelectedPlaybackEngineTypeAccess(this, new DefaultPlaybackEngineLookup()),
						handler,
						new BestMatchUriProvider(
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
								new MediaQueryCursorProvider(this, cachedFilePropertiesProvider),
								arbitratorForOs,
								library,
								false),
							remoteFileUriProvider),
						extractorMediaSourceFactoryProvider,
						exoPlayer,
						mediaSourceQueue,
						lazyRenderersFactory.getObject(),
						new AudioTrackVolumeManager(
							exoPlayer,
							Stream.of(renderers)
								.filter(r -> r instanceof MediaCodecAudioRenderer)
								.toArray(MediaCodecAudioRenderer[]::new)));

				return playbackEngineBuilder.build(library);
			}).then(preparationSourceProvider -> {
				if (preparedPlaybackQueueResourceManagement != null)
					preparedPlaybackQueueResourceManagement.close();

				preparedPlaybackQueueResourceManagement = new PreparedPlaybackQueueResourceManagement(
					preparationSourceProvider,
					preparationSourceProvider);

				if (playbackQueues != null)
					playbackQueues.close();

				playbackQueues = new PreparedPlaybackQueueResourceManagement(preparationSourceProvider, preparationSourceProvider);

				playbackEngine =
					new PlaybackEngine(
						playbackQueues,
						QueueProviders.providers(),
						new NowPlayingRepository(
							new SpecificLibraryProvider(
								lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(),
								lazyLibraryRepository.getObject()),
							lazyLibraryRepository.getObject()),
						playlistPlaybackBootstrapper);

				playbackEngine
					.setOnPlaybackStarted(this::handlePlaybackStarted)
					.setOnPlayingFileChanged(this::changePositionedPlaybackFile)
					.setOnPlaylistError(this::uncaughtExceptionHandler)
					.setOnPlaybackCompleted(this::onPlaylistPlaybackComplete)
					.setOnPlaylistReset(this::broadcastResetPlaylist);

				return playbackEngine;
			});
		}, e -> {
			localBroadcastManagerLazy.getObject().unregisterReceiver(buildSessionReceiver);
			throw e;
		});
	}

	private void handleBuildConnectionStatusChange(final int status) {
		final Builder notifyBuilder = new Builder(this, lazyPlaybackNotificationsConfiguration.getObject().getNotificationChannel());
		notifyBuilder.setContentTitle(getText(R.string.title_svc_connecting_to_server));
		switch (status) {
			case BuildingSessionConnectionStatus.GettingLibrary:
				notifyBuilder.setContentText(getText(R.string.lbl_getting_library_details));
				break;
			case BuildingSessionConnectionStatus.GettingLibraryFailed:
				Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_please_connect_to_valid_server), Toast.LENGTH_SHORT).show();
				return;
			case BuildingSessionConnectionStatus.BuildingConnection:
				notifyBuilder.setContentText(getText(R.string.lbl_connecting_to_server_library));
				break;
			case BuildingSessionConnectionStatus.BuildingConnectionFailed:
				Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_error_connecting_try_again), Toast.LENGTH_SHORT).show();
				return;
			case BuildingSessionConnectionStatus.GettingView:
				notifyBuilder.setContentText(getText(R.string.lbl_getting_library_views));
				break;
			case BuildingSessionConnectionStatus.GettingViewFailed:
				Toast.makeText(this, PlaybackService.this.getText(R.string.lbl_library_no_views), Toast.LENGTH_SHORT).show();
				return;
		}
		notifyNotificationManager(notifyBuilder);
	}

	private void handlePlaybackStarted(PositionedPlayingFile positionedPlayableFile) {
		isPlaying = true;
		lazyPlaybackStartedBroadcaster.getObject().broadcastPlaybackStarted(positionedPlayableFile.asPositionedFile());
	}

	private void pausePlayback(boolean isUserInterrupted) {
		isPlaying = false;

		if (isUserInterrupted && areListenersRegistered) unregisterListeners();

		if (playbackEngine == null) return;

		playbackEngine.pause();

		if (positionedPlayingFile != null)
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistPause, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

		if (filePositionSubscription != null)
			filePositionSubscription.dispose();
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

		if (exception instanceof PlaybackException) {
			handlePlaybackException((PlaybackException)exception);
			return;
		}

		logger.error("An unexpected error has occurred!", exception);
		stopNotification();
	}

	private void handlePlaybackEngineInitializationException(PlaybackEngineInitializationException exception) {
		logger.error("There was an error initializing the playback engine", exception);
		stopNotification();
	}

	private void handlePreparationException(PreparationException preparationException) {
		logger.error("An error occurred during file preparation for file " + preparationException.getPositionedFile().getServiceFile(), preparationException);
		uncaughtExceptionHandler(preparationException.getCause());
	}

	private void handlePlaybackException(PlaybackException exception) {
		if (exception.getCause() instanceof IllegalStateException) {
			logger.error("The player ended up in an illegal state - closing and restarting the player", exception);
			closeAndRestartPlaylistManager();

			return;
		}

		if (exception.getCause() instanceof IOException) {
			handleIoException((IOException)exception.getCause());
			return;
		}

		logger.error("An unexpected playback exception occurred", exception);
	}

	private void handleIoException(IOException exception) {
		logger.error("An IO exception occurred during playback", exception);
		handleDisconnection();
	}

	private void handleDisconnection() {
		final long currentErrorTime = System.currentTimeMillis();
		// Stop handling errors if more than the max errors has occurred
		if (++numberOfErrors > maxErrors) {
			// and the last error time is less than the error count reset duration
			if (currentErrorTime <= lastErrorTime + errorCountResetDuration) {
				logger.warn("Number of errors has not surpassed " + maxErrors + " in less than " + errorCountResetDuration + "ms. Closing and restarting playlist manager.");

				closeAndRestartPlaylistManager();
				return;
			}

			// reset the error count if enough time has elapsed to reset the error count
			numberOfErrors = 1;
		}

		lastErrorTime = currentErrorTime;

		final Builder builder = new Builder(this, lazyPlaybackNotificationsConfiguration.getObject().getNotificationChannel());
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		final Intent intent = new Intent(this, PlaybackService.class);
		intent.setAction(Action.stopWaitingForConnection);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);

		builder.setContentTitle(getText(R.string.lbl_waiting_for_connection));
		builder.setContentText(getText(R.string.lbl_click_to_cancel));
		notifyBackground(builder);

		pollingSessionConnection = PollConnectionService.pollSessionConnection(this);
		pollingSessionConnection
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
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onFileStart, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

		final ProgressingPromise<Duration, PlayedFile> promisedPlayedFile = playingFile.promisePlayedFile();
		final Disposable localSubscription = filePositionSubscription =
			Observable.interval(1, TimeUnit.SECONDS, lazyObservationScheduler.getObject())
				.map(t -> promisedPlayedFile.getProgress())
				.distinctUntilChanged()
				.subscribe(new TrackPositionBroadcaster(
					localBroadcastManagerLazy.getObject(),
					playingFile));


		promisedPlayedFile.then(p -> {
			lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onFileComplete, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());
			localSubscription.dispose();
			return null;
		});

		if (!areListenersRegistered) registerListeners();
		registerRemoteClientControl();
	}

	private void broadcastResetPlaylist(PositionedFile positionedFile) {
		lazyPlaybackBroadcaster.getObject()
			.sendPlaybackBroadcast(
				PlaylistEvents.onPlaylistChange,
				lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(),
				positionedFile);
	}

	private Void broadcastChangedFile(PositionedFile positionedFile) {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistChange, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedFile);
		return null;
	}

	private void onPlaylistPlaybackComplete() {
		lazyPlaybackBroadcaster.getObject().sendPlaybackBroadcast(PlaylistEvents.onPlaylistStop, lazyChosenLibraryIdentifierProvider.getObject().getSelectedLibraryId(), positionedPlayingFile.asPositionedFile());

		killService(this);
	}
		
	@Override
	public void onDestroy() {
		stopNotification();

		localBroadcastManagerLazy.getObject().unregisterReceiver(onLibraryChanged);
		localBroadcastManagerLazy.getObject().unregisterReceiver(onPlaybackEngineChanged);

		if (playlistPlaybackBootstrapper != null) {
			try {
				playlistPlaybackBootstrapper.close();
			} catch (IOException e) {
				logger.error("There was an error closing the playback bootstrapper", e);
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

		if (preparedPlaybackQueueResourceManagement != null) {
			preparedPlaybackQueueResourceManagement.close();
		}

		if (areListenersRegistered) unregisterListeners();

		if (remoteControlProxy != null)
			localBroadcastManagerLazy.getObject().unregisterReceiver(remoteControlProxy);

		if (playbackNotificationRouter != null)
			localBroadcastManagerLazy.getObject().unregisterReceiver(playbackNotificationRouter);

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

		if (cache != null) {
			cache.release();
		}

		if (nowPlayingNotificationBuilder != null)
			nowPlayingNotificationBuilder.close();
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
		private static final String stopWaitingForConnection = magicPropertyBuilder.buildProperty("stopWaitingForConnection");
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
			stopWaitingForConnection,
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
