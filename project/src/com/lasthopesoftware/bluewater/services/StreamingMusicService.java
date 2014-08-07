/**
 * 
 */
package com.lasthopesoftware.bluewater.services;


import java.util.HashSet;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.SparseArray;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.access.ImageTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionLostListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnPollingCancelledListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.receivers.RemoteControlReceiver;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements
	OnAudioFocusChangeListener, 
	OnNowPlayingChangeListener, 
	OnNowPlayingStartListener,
	OnNowPlayingStopListener,
	OnNowPlayingPauseListener, 
	OnPlaylistStateControlErrorListener
{
	/* String constant actions */
	private static final String ACTION_START = "com.lasthopesoftware.bluewater.ACTION_START";
	private static final String ACTION_PLAY = "com.lasthopesoftware.bluewater.ACTION_PLAY";
	private static final String ACTION_PAUSE = "com.lasthopesoftware.bluewater.ACTION_PAUSE";
	private static final String ACTION_PREVIOUS = "com.lasthopesoftware.bluewater.ACTION_PREVIOUS";
	private static final String ACTION_NEXT = "com.lasthopesoftware.bluewater.ACTION_NEXT";
	private static final String ACTION_SEEK_TO = "com.lasthopesoftware.bluewater.ACTION_SEEK_TO";
	private static final String ACTION_SYSTEM_PAUSE = "com.lasthopesoftware.bluewater.ACTION_SYSTEM_PAUSE";
	private static final String ACTION_STOP_WAITING_FOR_CONNECTION = "com.lasthopesoftware.bluewater.ACTION_STOP_WAITING_FOR_CONNECTION";
	private static final String ACTION_INITIALIZE_PLAYLIST = "com.lasthopesoftware.bluewater.ACTION_INITIALIZE_PLAYLIST";
	
	/* Bag constants */
	private static final String BAG_FILE_KEY = "com.lasthopesoftware.bluewater.bag.FILE_KEY";
	private static final String BAG_PLAYLIST = "com.lasthopesoftware.bluewater.bag.FILE_PLAYLIST";
	private static final String BAG_START_POS = "com.lasthopesoftware.bluewater.bag.START_POS";
	
	/* Miscellaneous programming related string constants */
	private static final String PEBBLE_NOTIFY_INTENT = "com.getpebble.action.NOW_PLAYING";
	private static final String WIFI_LOCK_SVC_NAME =  "project_blue_water_svc_lock";
		
	private static int mId = 42;
	private static int mStartId;
	private WifiLock mWifiLock = null;
	private NotificationManager mNotificationMgr;
	private Context mThis;
	private AudioManager mAudioManager;
	private ComponentName mRemoteControlReceiver;
	private RemoteControlClient mRemoteControlClient;
	private Library mLibrary;
	
	// State dependent static variables
	private static String mPlaylistString;
	private static PlaylistController mPlaylistController;
	
	// State dependent non-static variables
	private static boolean mAreListenersRegistered = false;
	private static boolean mIsNotificationForeground = false;
	
	private static final Object syncHandlersObject = new Object();
	private static final Object syncPlaylistControllerObject = new Object();
	
	private static final HashSet<OnNowPlayingChangeListener> mOnStreamingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private static final HashSet<OnNowPlayingStartListener> mOnStreamingStartListeners = new HashSet<OnNowPlayingStartListener>();
	private static final HashSet<OnNowPlayingStopListener> mOnStreamingStopListeners = new HashSet<OnNowPlayingStopListener>();
	private static final HashSet<OnNowPlayingPauseListener> mOnStreamingPauseListeners = new HashSet<OnNowPlayingPauseListener>();
	
	private final OnConnectionLostListener mConnectionLostListener = new OnConnectionLostListener() {
		
		@Override
		public void onConnectionLost() {
			if (mPlaylistController != null) mPlaylistController.pause();
			
			buildErrorNotification();
		}
	};
	
	private final OnConnectionRegainedListener mConnectionRegainedListener = new OnConnectionRegainedListener() {
		
		@Override
		public void onConnectionRegained() {
			if (mLibrary == null) {
				stopSelf();
				return;
			}

			startPlaylist(mLibrary.getSavedTracksString(), mLibrary.getNowPlayingId(), mLibrary.getNowPlayingProgress());
		}
	};
	
	private static Intent getNewSelfIntent(final Context context, String action) {
		final Intent newIntent = new Intent(context, StreamingMusicService.class);
		newIntent.setAction(action);
		return newIntent;
	}
	
	/* Begin streamer intent helpers */
	public static void resumeSavedPlaylist(final Context context) {
		JrSession.GetLibrary(context, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null || result.getSavedTracksString() == null) return;
				initializePlaylist(context, result.getNowPlayingId(), result.getNowPlayingProgress(), result.getSavedTracksString());
			}
		});
	}
	
	public static void initializePlaylist(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_INITIALIZE_PLAYLIST);		
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}
	
	public static void initializePlaylist(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
	}
	
	public static void initializePlaylist(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_INITIALIZE_PLAYLIST);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, fileProgress);
		context.startService(svcIntent);
	}
	
	public static void streamMusic(final Context context, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_START);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(final Context context, int filePos, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(final Context context, int filePos, int fileProgress, String serializedFileList) {
		final Intent svcIntent = getNewSelfIntent(context, ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, fileProgress);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void streamMusic(final Context context, int filePos) { 
		final Intent svcIntent = getNewSelfIntent(context, ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void streamMusic(final Context context, int filePos, int fileProgress) { 
		final Intent svcIntent = getNewSelfIntent(context, ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, filePos);
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		svcIntent.putExtra(BAG_START_POS, fileProgress);
		context.startService(svcIntent);
	}
	
	public static void play(final Context context) {
		context.startService(getNewSelfIntent(context, ACTION_PLAY));
	}
	
	public static void pause(final Context context) {
		context.startService(getNewSelfIntent(context, ACTION_PAUSE));
	}
	
	public static void next(final Context context) {
		context.startService(getNewSelfIntent(context, ACTION_NEXT));
	}
	
	public static void previous(final Context context) {
		context.startService(getNewSelfIntent(context, ACTION_PREVIOUS));
	}
	
	public static void setIsRepeating(final Context context, final boolean isRepeating) {
		JrSession.GetLibrary(context, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;
				result.setRepeating(isRepeating);
				JrSession.SaveSession(context);
				if (mPlaylistController != null) mPlaylistController.setIsRepeating(isRepeating);
			}
		});
	}
	
	/* End streamer intent helpers */
	
	/* Begin Events */
	public static void addOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		mOnStreamingChangeListeners.add(listener);
	}

	public static void addOnStreamingStartListener(OnNowPlayingStartListener listener) {
		mOnStreamingStartListeners.add(listener);
	}
	
	public static void addOnStreamingStopListener(OnNowPlayingStopListener listener) {
		mOnStreamingStopListeners.add(listener);
	}
	
	public static void addOnStreamingPauseListener(OnNowPlayingPauseListener listener) {
		mOnStreamingPauseListeners.add(listener);
	}
	
	public static void removeOnStreamingChangeListener(OnNowPlayingChangeListener listener) {
		synchronized(syncHandlersObject) {
			if (mOnStreamingChangeListeners.contains(listener))
				mOnStreamingChangeListeners.remove(listener);
		}
	}

	public static void removeOnStreamingStartListener(OnNowPlayingStartListener listener) {
		synchronized(syncHandlersObject) {
			if (mOnStreamingStartListeners.contains(listener))
				mOnStreamingStartListeners.remove(listener);
		}
	}
	
	public static void removeOnStreamingStopListener(OnNowPlayingStopListener listener) {
		synchronized(syncHandlersObject) {
			if (mOnStreamingStopListeners.contains(listener))
				mOnStreamingStopListeners.remove(listener);
		}
	}
	
	public static void removeOnStreamingPauseListener(OnNowPlayingPauseListener listener) {
		synchronized(syncHandlersObject) {
			if (mOnStreamingPauseListeners.contains(listener))
				mOnStreamingPauseListeners.remove(listener);
		}
	}
	
	private void throwChangeEvent(PlaylistController controller, FilePlayer filePlayer) {
		synchronized(syncHandlersObject) {
			for (OnNowPlayingChangeListener onChangeListener : mOnStreamingChangeListeners)
				onChangeListener.onNowPlayingChange(controller, filePlayer);
		}
	}

	private void throwStartEvent(PlaylistController controller, FilePlayer filePlayer) {
		synchronized(syncHandlersObject) {
			for (OnNowPlayingStartListener onStartListener : mOnStreamingStartListeners)
				onStartListener.onNowPlayingStart(controller, filePlayer);
		}
	}
	
	private void throwStopEvent(PlaylistController controller, FilePlayer filePlayer) {
		synchronized(syncHandlersObject) {
			for (OnNowPlayingStopListener onStopListener : mOnStreamingStopListeners)
				onStopListener.onNowPlayingStop(controller, filePlayer);
		}
	}
	
	private void throwPauseEvent(PlaylistController controller, FilePlayer filePlayer) {
		synchronized(syncHandlersObject) {
			for (OnNowPlayingPauseListener onPauseListener : mOnStreamingPauseListeners)
				onPauseListener.onNowPlayingPause(controller, filePlayer);
		}
	}
	/* End Events */
		
	public static PlaylistController getPlaylistController() {
		synchronized(syncPlaylistControllerObject) {
			return mPlaylistController;
		}
	}
	
	public StreamingMusicService() {
		super();
		mThis = this;
		JrSession.GetLibrary(mThis, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				mLibrary = result;
			}
			
		});
	}
	
	private void startPlaylist(String playlistString, int filePos, int fileProgress) {
		// If the playlist has changed, change that
		if (mPlaylistController == null || !playlistString.equals(mPlaylistString)) {
			initializePlaylist(playlistString);
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		builder.setContentTitle(String.format(getString(R.string.lbl_starting_service), getString(R.string.app_name)));
        
		notifyForeground(builder.build());
        
        mPlaylistController.startAt(filePos, fileProgress);
	}
	
	private void restorePlaylistControllerFromStorage() {
		if (mLibrary != null) {

			ConnectionManager.refreshConfiguration(mThis, new OnCompleteListener<Integer, Void, Boolean>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
					initializePlaylist(mLibrary.getSavedTracksString(), mLibrary.getNowPlayingId(), mLibrary.getNowPlayingProgress());
				}
				
			});
			
			return;
		}
		
		JrSession.GetLibrary(mThis, new ISimpleTask.OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (result == null) return;
				mLibrary = result;
				
				restorePlaylistControllerFromStorage();					
			}
			
		});
	}
	
	private void initializePlaylist(String playlistString, int filePos, int fileProgress) {
		initializePlaylist(playlistString);
		
		if (!playlistString.isEmpty())
			mPlaylistController.seekTo(filePos, fileProgress);
	}
		
	private void initializePlaylist(String playlistString) {
		mPlaylistString = playlistString;
		
		// First try to get the playlist string from the database
		if (mPlaylistString == null || mPlaylistString.isEmpty()) mPlaylistString = mLibrary.getSavedTracksString();
		
		mLibrary.setSavedTracksString(mPlaylistString);
		JrSession.SaveSession(mThis);
		
		if (mPlaylistController != null) {
			mPlaylistController.pause();
			mPlaylistController.release();
		}
		
		synchronized(syncPlaylistControllerObject) {
			mPlaylistController = new PlaylistController(mThis, mPlaylistString);
		}
		mPlaylistController.setIsRepeating(mLibrary.isRepeating());
		mPlaylistController.addOnNowPlayingChangeListener(this);
		mPlaylistController.addOnNowPlayingStopListener(this);
		mPlaylistController.addOnPlaylistStateControlErrorListener(this);
		mPlaylistController.addOnNowPlayingStartListener(this);
	}
	
	private void pausePlayback(boolean isUserInterrupted) {
		if (mPlaylistController != null) {
			if (mPlaylistController.isPlaying()) {
				if (isUserInterrupted & mAreListenersRegistered) unregisterListeners();
				mPlaylistController.pause();
			}
		}
		stopNotification();
	}
	
	private void buildErrorNotification() {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		final Intent intent = new Intent(mThis, StreamingMusicService.class);
		intent.setAction(ACTION_STOP_WAITING_FOR_CONNECTION);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);
		
		final CharSequence waitingText = getText(R.string.lbl_waiting_for_connection);
		builder.setContentTitle(waitingText);
		builder.setTicker(waitingText);
		builder.setSubText(getText(R.string.lbl_click_to_cancel));
		notifyForeground(builder.build());
		PollConnection checkConnection = PollConnection.Instance.get(mThis);
		
		checkConnection.addOnConnectionRegainedListener(mConnectionRegainedListener);
		
		checkConnection.addOnPollingCancelledListener(new OnPollingCancelledListener() {
			
			@Override
			public void onPollingCancelled() {
				unregisterListeners();
				stopSelf();
			}
		});
		
		checkConnection.startPolling();
	}
	
	private void notifyForeground(Notification notification) {
		if (!mIsNotificationForeground) {
			startForeground(mId, notification);
			mIsNotificationForeground = true;
			return;
		}
		
		mNotificationMgr.notify(mId, notification);
	}
	
	private void stopNotification() {
		stopForeground(true);
		mIsNotificationForeground = false;
		mNotificationMgr.cancel(mId);
	}
	
	private void registerListeners() {
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		mRemoteControlReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
		mAudioManager.registerMediaButtonEventReceiver(mRemoteControlReceiver);
		
		mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_SVC_NAME);
        mWifiLock.acquire();
        
        // build the PendingIntent for the remote control client
		final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mRemoteControlReceiver);
		final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(mThis, 0, mediaButtonIntent, 0);
		// create and register the remote control client
		mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		mRemoteControlClient.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                RemoteControlClient.FLAG_KEY_MEDIA_STOP);
		
		mAudioManager.registerRemoteControlClient(mRemoteControlClient);
		
		PollConnection.Instance.get(mThis).addOnConnectionLostListener(mConnectionLostListener);
		
		mAreListenersRegistered = true;
	}
	
	private void unregisterListeners() {
		mAudioManager.abandonAudioFocus(this);
		if (mRemoteControlClient != null) mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
		if (mRemoteControlReceiver != null) mAudioManager.unregisterMediaButtonEventReceiver(mRemoteControlReceiver);
		// release the wifilock if we still have it
		if (mWifiLock != null) {
			if (mWifiLock.isHeld()) mWifiLock.release();
			mWifiLock = null;
		}
		final PollConnection pollConnection = PollConnection.Instance.get(mThis);
		pollConnection.removeOnConnectionLostListener(mConnectionLostListener);
		pollConnection.removeOnConnectionRegainedListener(mConnectionRegainedListener);
		
		mAreListenersRegistered = false;
	}
	
	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Should be modified to save its state locally in the future.
		mStartId = startId;
		
		if (mThis == null) mThis = this;
		
		if (mLibrary != null) {
			actOnIntent(intent);
		} else {
			final Intent _intent = intent;

			JrSession.GetLibrary(mThis, new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					mLibrary = result;
					actOnIntent(_intent);
				}
			});
		}
		
		return START_NOT_STICKY;
	}
	
	private void actOnIntent(final Intent intent) {
		if (intent != null) {
			// 3/5 times it's going to be this so let's see if we can get
			// some improved prefetching by the processor
				
			String action = intent.getAction(); 
			if (action.equals(ACTION_START)) {
				startPlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, 0));
	        } else if (action.equals(ACTION_INITIALIZE_PLAYLIST)) {
	        	initializePlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, 0));
	        } else if (action.equals(ACTION_PLAY)) {
	        	if (mPlaylistController == null || !mPlaylistController.resume())
	        		startPlaylist(mLibrary.getSavedTracksString(), mLibrary.getNowPlayingId(), mLibrary.getNowPlayingProgress());
	        } else if (action.equals(ACTION_PREVIOUS)) {
	        	if (mPlaylistController == null) restorePlaylistControllerFromStorage();
	        	mPlaylistController.seekTo(mPlaylistController.getCurrentPosition() - 1);	        	
	        } else if (action.equals(ACTION_NEXT)) {
	        	if (mPlaylistController == null) restorePlaylistControllerFromStorage();
	        	mPlaylistController.seekTo(mPlaylistController.getCurrentPosition() + 1);
	        } else if (mPlaylistController != null && action.equals(ACTION_PAUSE)) {
	        	pausePlayback(true);
	        } else if (action.equals(ACTION_STOP_WAITING_FOR_CONNECTION)) {
	        	PollConnection.Instance.get(mThis).stopPolling();
	        }
		} else if (mLibrary != null)  {
			pausePlayback(true);
		}
	}
	
	@Override
    public void onCreate() {
		mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	@Override
	public boolean onPlaylistStateControlError(PlaylistController controller, FilePlayer filePlayer) {
		PollConnection.Instance.get(mThis).startPolling();

		return true;
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
			if (mPlaylistController == null) return;
			mPlaylistController.setVolume(1.0f);
    		if (mPlaylistController.isPlaying()) return;
    		
        	ConnectionManager.refreshConfiguration(mThis, new OnCompleteListener<Integer, Void, Boolean>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
		        	startPlaylist(mLibrary.getSavedTracksString(), mLibrary.getNowPlayingId(), mLibrary.getNowPlayingProgress());
				}
        		
        	});
        	
            return;
		}
		
		if (mPlaylistController == null) return;
		
	    switch (focusChange) {
        	// Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS:
	        	if (mPlaylistController.isPlaying()) pausePlayback(true);
	        // Lost focus but it will be regained... cannot release resources
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	        	if (mPlaylistController.isPlaying()) pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (mPlaylistController.isPlaying()) mPlaylistController.setVolume(0.1f);
	            return;
	    }
	}
	
	@Override
	public void onNowPlayingStop(PlaylistController controller, FilePlayer filePlayer) {
		mLibrary.setNowPlayingId(controller.getCurrentPosition());
		mLibrary.setNowPlayingProgress(filePlayer.getCurrentPosition());
		JrSession.SaveSession(mThis);
		
		stopNotification();
		if (mAreListenersRegistered) unregisterListeners();
		
		throwStopEvent(controller, filePlayer);
	}
	

	@Override
	public void onNowPlayingPause(PlaylistController controller, FilePlayer filePlayer) {
		mLibrary.setNowPlayingId(controller.getCurrentPosition());
		mLibrary.setNowPlayingProgress(filePlayer.getCurrentPosition());
		JrSession.SaveSession(mThis);
		
		stopNotification();
		
		throwPauseEvent(controller, filePlayer);
	}

	@Override
	public void onNowPlayingChange(PlaylistController controller, FilePlayer filePlayer) {
		mLibrary.setNowPlayingId(controller.getCurrentPosition());
		mLibrary.setNowPlayingProgress(filePlayer.getCurrentPosition());
		JrSession.SaveSession(mThis);
		throwChangeEvent(controller, filePlayer);
	}

	@Override
	public void onNowPlayingStart(PlaylistController controller, FilePlayer filePlayer) {
		final File playingFile = filePlayer.getFile();
		
		if (!mAreListenersRegistered) registerListeners();
		
		// Set the notification area
		final Intent viewIntent = new Intent(this, ViewNowPlaying.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);
		
		final SimpleTask<Void, Void, String> getNotificationPropertiesTask = new SimpleTask<Void, Void, String>();
		getNotificationPropertiesTask.setOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
			
			@Override
			public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				return playingFile.getProperty(FileProperties.ARTIST) + " - " + playingFile.getValue();
			}
		});
		getNotificationPropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				NotificationCompat.Builder builder = new NotificationCompat.Builder(mThis);
		        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
				builder.setOngoing(true);
				builder.setContentTitle(String.format(getString(R.string.title_svc_now_playing), getText(R.string.app_name)));
				builder.setContentText(result == null ? "Error getting file properties." : result);
				builder.setContentIntent(pi);
				notifyForeground(builder.build());
			}
		});
		
		getNotificationPropertiesTask.execute();
		
		final SimpleTask<Void, Void, SparseArray<Object>> getBtPropertiesTask = new SimpleTask<Void, Void, SparseArray<Object>>();
		getBtPropertiesTask.setOnExecuteListener(new OnExecuteListener<Void, Void, SparseArray<Object>>() {
			
			@Override
			public SparseArray<Object> onExecute(ISimpleTask<Void, Void, SparseArray<Object>> owner, Void... params) throws Exception {
				SparseArray<Object> result = new SparseArray<Object>(4);
				result.put(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingFile.getProperty(FileProperties.ARTIST));
				result.put(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingFile.getProperty(FileProperties.ALBUM));
				result.put(MediaMetadataRetriever.METADATA_KEY_TITLE, playingFile.getValue());
				result.put(MediaMetadataRetriever.METADATA_KEY_DURATION, Long.valueOf(playingFile.getDuration()));
				return result;
			}
		});
		
		getBtPropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, SparseArray<Object>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, SparseArray<Object>> owner, SparseArray<Object> result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				final String artist = (String)result.get(MediaMetadataRetriever.METADATA_KEY_ARTIST);
				final String album = (String)result.get(MediaMetadataRetriever.METADATA_KEY_ALBUM);
				final String title = (String)result.get(MediaMetadataRetriever.METADATA_KEY_TITLE);
				
				if (mRemoteControlClient != null) {
					final MetadataEditor metaData = mRemoteControlClient.editMetadata(true);
					metaData.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, artist);
					metaData.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, album);
					metaData.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title);				
					metaData.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, (Long)result.get(MediaMetadataRetriever.METADATA_KEY_DURATION));
					metaData.apply();
					
					final ImageTask getBtImageTask = new ImageTask(playingFile);
				    getBtImageTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Bitmap>() {
						
						@TargetApi(Build.VERSION_CODES.KITKAT)
						@Override
						public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
							if (result == null || android.os.Build.VERSION.SDK_INT < 19) return;
							
							final MetadataEditor metaData = mRemoteControlClient.editMetadata(false);
							metaData.putBitmap(MediaMetadataEditor.BITMAP_KEY_ARTWORK, result);
							metaData.apply();
						}
					});
				    
				    getBtImageTask.execute();
				}
				
				final Intent pebbleIntent = new Intent(PEBBLE_NOTIFY_INTENT);
				pebbleIntent.putExtra("artist", artist);
				pebbleIntent.putExtra("album", album);
				pebbleIntent.putExtra("track", title);
			    
			    sendBroadcast(pebbleIntent);
			}
		});
		getBtPropertiesTask.execute();
		
		throwStartEvent(controller, filePlayer);
	}
	
	@Override
	public void onDestroy() {
		JrSession.SaveSession(this);
		
		stopNotification();
		
		if (mPlaylistController != null) {
			mPlaylistController.release();
			mPlaylistController = null;
		}
		
		if (mAreListenersRegistered) unregisterListeners();
		
		mPlaylistString = null;
		
		PollConnection.Instance.get(mThis).removeOnConnectionLostListener(mConnectionLostListener);
	}

	/* End Event Handlers */
	
	/* Begin Binder Code */
	
	public class StreamingMusicServiceBinder extends Binder {
        StreamingMusicService getService() {
            return StreamingMusicService.this;
        }
    }

    private final IBinder mBinder = new StreamingMusicServiceBinder();
	/* End Binder Code */

}
