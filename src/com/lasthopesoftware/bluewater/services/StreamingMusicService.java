/**
 * 
 */
package com.lasthopesoftware.bluewater.services;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.slf4j.LoggerFactory;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewNowPlaying;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFilePreparedListener;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;


/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements OnJrFilePreparedListener, OnJrFileErrorListener, 
		OnJrFileCompleteListener, OnAudioFocusChangeListener {
	
	//private final IBinder mBinder = 
	public static final String ACTION_START = "com.lasthopesoftware.bluewater.ACTION_START";
	public static final String ACTION_PLAY = "com.lasthopesoftware.bluewater.ACTION_PLAY";
	public static final String ACTION_STOP = "com.lasthopesoftware.bluewater.ACTION_STOP";
	public static final String ACTION_PAUSE = "com.lasthopesoftware.bluewater.ACTION_PAUSE";
	public static final String ACTION_SYSTEM_PAUSE = "com.lasthopesoftware.bluewater.ACTION_SYSTEM_PAUSE";
	public static final String ACTION_STOP_WAITING_FOR_CONNECTION = "com.lasthopesoftware.bluewater.ACTION_STOP_WAITING_FOR_CONNECTION";
	
	private static final String BAG_FILE_KEY = "com.lasthopesoftware.bluewater.bag.FILE_KEY";
	private static final String BAG_PLAYLIST = "com.lasthopesoftware.bluewater.bag.FILE_PLAYLIST";
	private static final String BAG_START_POS = "com.lasthopesoftware.bluewater.bag.START_POS";
	
	private static int mId = 42;
	private static int mStartId;
	private WifiLock mWifiLock = null;
	private int mFileKey = -1;
	private int mStartPos = 0;
	private NotificationManager mNotificationMgr;
	private Thread trackProgressThread;
	private static ArrayList<JrFile> mPlaylist;
	private static JrFile mPlayingFile;
	private static String mPlaylistString;
	private Context thisContext;
	private AudioManager mAudioManager;
	
	private static Object syncObject = new Object();
	
	private static HashSet<OnStreamingStartListener> mOnStreamingStartListeners = new HashSet<OnStreamingStartListener>();
	private static HashSet<OnStreamingStopListener> mOnStreamingStopListeners = new HashSet<OnStreamingStopListener>();
	
	/* Begin streamer intent helpers */
	
	public static void StreamMusic(Context context, int startFileKey, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void StreamMusic(Context context, int startFileKey, int startPos, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, startFileKey);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		svcIntent.putExtra(BAG_START_POS, startPos);
		context.startService(svcIntent);
	}
	
	public static void StreamMusic(Context context, String serializedFileList) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_PLAYLIST, serializedFileList);
		context.startService(svcIntent);
		ViewUtils.CreateNowPlayingView(context);
	}
	
	public static void Play(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY);
		context.startService(svcIntent);
	}
	
	public static void Pause(Context context) {
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_PAUSE);
		context.startService(svcIntent);
	}
	
	public static void Next(Context context) {
		JrFile nextFile = mPlayingFile.getNextFile();
		if (nextFile == null) return;
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, nextFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	public static void Previous(Context context) {
		JrFile previousFile = mPlayingFile.getPreviousFile();
		if (previousFile == null) return;
		Intent svcIntent = new Intent(StreamingMusicService.ACTION_START);
		svcIntent.putExtra(BAG_FILE_KEY, previousFile.getKey());
		svcIntent.putExtra(BAG_PLAYLIST, mPlaylistString);
		context.startService(svcIntent);
	}
	
	/* End streamer intent helpers */
	
	/* Begin Events */
	public static void AddOnStreamingStartListener(OnStreamingStartListener listener) {
		mOnStreamingStartListeners.add(listener);
	}
	
	public static void AddOnStreamingStopListener(OnStreamingStopListener listener) {
		mOnStreamingStopListeners.add(listener);
	}
	
	public static void RemoveOnStreamingStartListener(OnStreamingStartListener listener) {
		synchronized(syncObject) {
			if (mOnStreamingStartListeners.contains(listener))
				mOnStreamingStartListeners.remove(listener);
		}
	}
	
	public static void RemoveOnStreamingStopListener(OnStreamingStopListener listener) {
		synchronized(syncObject) {
			if (mOnStreamingStopListeners.contains(listener))
				mOnStreamingStopListeners.remove(listener);
		}
	}
	
	private void throwStartEvent(JrFile file) {
		synchronized(syncObject) {
			for (OnStreamingStartListener onStartListener : mOnStreamingStartListeners)
				onStartListener.onStreamingStart(this, file);
		}
	}
	
	private void throwStopEvent(JrFile file) {
		synchronized(syncObject) {
			for (OnStreamingStopListener onStopListener : mOnStreamingStopListeners)
				onStopListener.onStreamingStop(this, file);
		}
	}
	/* End Events */
	
	public static JrFile getNowPlayingFile() {
		return mPlayingFile;
	}
	
	public StreamingMusicService() {
		super();
		thisContext = this;
	}

	private void startFilePlayback(JrFile file) {
		final JrFile playingFile = file;
		mPlayingFile = playingFile;
		mFileKey = playingFile.getKey();
		JrSession.GetLibrary(thisContext).setNowPlayingId(mFileKey);
		JrSession.SaveSession(thisContext);
		// Start playback immediately
		playingFile.start();
		// Set the notification area
		Intent viewIntent = new Intent(this, ViewNowPlaying.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, viewIntent, 0);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "project_blue_water_svc_lock");
        mWifiLock.acquire();
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		
		SimpleTask<Void, Void, String> getFilePropertiesTask = new SimpleTask<Void, Void, String>();
		getFilePropertiesTask.addOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
			
			@Override
			public void onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				owner.setResult(playingFile.getProperty("Artist") + " - " + playingFile.getValue());
			}
		});
		getFilePropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				NotificationCompat.Builder builder = new NotificationCompat.Builder(thisContext);
		        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
				builder.setOngoing(true);
				builder.setContentTitle("Music Streamer Now Playing");
				builder.setContentText(result == null ? "Error getting file properties." : result);
				builder.setContentIntent(pi);
				mNotificationMgr.notify(mId, builder.build());
			}
		});
		
		getFilePropertiesTask.execute();
        
        if (playingFile.getNextFile() != null) {
        	BackgroundFilePreparer backgroundProgressThread = new BackgroundFilePreparer(this, playingFile);
        	if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
	        trackProgressThread = new Thread(backgroundProgressThread);
	        trackProgressThread.setName("Thread to prepare next file.");
	        trackProgressThread.setPriority(Thread.MIN_PRIORITY);
	        trackProgressThread.start();
        }
        
        throwStartEvent(file);
	}
	
	private void startPlaylist(String playlistString, int fileKey) {
		startPlaylist(playlistString, fileKey, 0);
	}
	
	private void startPlaylist(String playlistString, int fileKey, int filePos) {
		if (playlistString == null) return;
		// If everything is the same as before, and stuff is playing, don't do anything else 
		if (playlistString.equals(mPlaylistString) && mFileKey == fileKey && mPlayingFile.isPlaying()) return;
		
		// stop any playback that is in action
		if (mPlayingFile != null) {
			if (mPlayingFile.isPlaying())
				mPlayingFile.stop();
			
			throwStopEvent(mPlayingFile);
			
			mPlayingFile = null;
			releaseMediaPlayers();
		}
		
		// If the playlist has changed, change that
		if (!playlistString.equals(mPlaylistString)) {
			mPlaylistString = playlistString;
			mPlaylist = JrFiles.deserializeFileStringList(mPlaylistString);
			JrSession.GetLibrary(thisContext).setSavedTracks(mPlaylist);
		}
		
		mFileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
		mStartPos = filePos < 0 ? 0 : filePos;
        
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		builder.setContentTitle("Starting Music Streamer");
        startForeground(mId, builder.build());
        
		for (JrFile file : mPlaylist) {
			if (file.getKey() != mFileKey) continue;
			
			file.addOnJrFileCompleteListener(this);
			file.addOnJrFilePreparedListener(this);
			file.addOnJrFileErrorListener(this);
        	file.initMediaPlayer(this);
        	file.seekTo(mStartPos);
        	file.prepareMediaPlayer(); // prepare async to not block main thread
        	break;
		}
	}
	
	private void pausePlayback(boolean isUserInterrupted) {
		if (mPlayingFile != null) {
			if (mPlayingFile.isPlaying()) {
				if (isUserInterrupted) mAudioManager.abandonAudioFocus(this);
				mPlayingFile.pause();
				JrSession.GetLibrary(thisContext).setNowPlayingId(mPlayingFile.getKey());
				JrSession.GetLibrary(thisContext).setNowPlayingProgress(mPlayingFile.getCurrentPosition());
			}
			JrSession.SaveSession(this);
			throwStopEvent(mPlayingFile);
		}
//		mPlaylistString = null;
//		mFileKey = -1;
		if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
		releaseMediaPlayers();
		stopNotification();
		if (isUserInterrupted) stopSelfResult(mStartId);
	}
	
	private void buildErrorNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_water_drop_white);
		builder.setOngoing(true);
		// Add intent for canceling waiting for connection to come back
		Intent intent = new Intent(ACTION_STOP_WAITING_FOR_CONNECTION);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);
		
		builder.setContentTitle("Waiting for Connection.");
		builder.setTicker("Waiting for Connection.");
		builder.setSubText("Click here to cancel.");
		mNotificationMgr.notify(mId, builder.build());
		PollConnectionTask checkConnection = PollConnectionTask.Instance.get();
		
		checkConnection.addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
				mNotificationMgr.cancelAll();
				if (result == Boolean.TRUE && JrSession.GetLibrary(thisContext) != null)
					StreamMusic(thisContext, mPlayingFile.getKey(), mPlayingFile.getCurrentPosition(), mPlaylistString);							
			}
		});
		
		checkConnection.startPolling();
	}
	
	private void stopNotification() {
		stopForeground(true);
		mNotificationMgr.cancel(mId);
	}
	
	private void releaseMediaPlayer(JrFile file) {
		file.releaseMediaPlayer();
		file.removeOnJrFileCompleteListener(this);
		file.removeOnJrFileErrorListener(this);
		file.removeOnJrFilePreparedListener(this);
	}
	
	private void releaseMediaPlayers() {
		if (mPlaylist == null) return;
		for (JrFile file : mPlaylist) releaseMediaPlayer(file);
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		/* Should be modified to save its state locally in the future.
		 */
		mStartId = startId;
		
		if (PollConnectionTask.Instance.get().isRunning()) return START_NOT_STICKY;
		if (intent != null) {
			// 3/5 times it's going to be this so let's see if we can get
			// some improved prefetching by the processor
			if (intent.getAction().equals(ACTION_START)) {
				startPlaylist(intent.getStringExtra(BAG_PLAYLIST), intent.getIntExtra(BAG_FILE_KEY, -1), intent.getIntExtra(BAG_START_POS, -1));
	        } else if (mPlaylist != null && mPlayingFile != null) {
	        	// These actions can only occur if mPlaylist and the PlayingFile are not null
	        	if (intent.getAction().equals(ACTION_PAUSE)) {
	        		pausePlayback(true);
		        } else if (intent.getAction().equals(ACTION_PLAY) && mPlayingFile != null) {
		    		if (!mPlayingFile.isMediaPlayerCreated()) startPlaylist(mPlaylistString, mPlayingFile.getKey(), mPlayingFile.getCurrentPosition());
		    		else startFilePlayback(mPlayingFile);
		        } else if (intent.getAction().equals(ACTION_STOP)) {
		        	pausePlayback(true);
		        }
	        } 
	        else if (intent.getAction().equals(ACTION_STOP_WAITING_FOR_CONNECTION)) {
	        	PollConnectionTask.Instance.get().stopPolling();
	        }
		} else if (!JrSession.isActive()) {
			if (JrSession.GetLibrary(thisContext) != null) pausePlayback(true);
		}
		return START_NOT_STICKY;
	}
	
	@Override
    public void onCreate() {
		mNotificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	}
	
	public void onJrFilePrepared(JrFile file) {
		if (!file.isPlaying()) startFilePlayback(file);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onJrFileError(JrFile file, int what, int extra) {
		LoggerFactory.getLogger(StreamingMusicService.class).error("JR File error - " + what + " - " + extra);
		mPlayingFile = file;		
		pausePlayback(false);
		
		buildErrorNotification();

		return true;
	}

	@Override
	public void onJrFileComplete(JrFile file) {		
		mAudioManager.abandonAudioFocus(this);
		// release the wifilock if we still have it
		if (mWifiLock != null) {
			if (mWifiLock.isHeld()) mWifiLock.release();
			mWifiLock = null;
		}
		JrFile nextFile = file.getNextFile();
		
		throwStopEvent(file);
		
		releaseMediaPlayer(file);
		
		if (nextFile == null) return;
		
		nextFile.addOnJrFileCompleteListener(this);
		nextFile.addOnJrFileErrorListener(this);
		if (!nextFile.isPrepared()) {
			nextFile.addOnJrFilePreparedListener(this);
			nextFile.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(nextFile);
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
			// resume playback
        	if (JrSession.GetLibrary(thisContext) != null && !JrSession.isActive()) return;
        	
        	if (mPlayingFile != null)
        		mPlayingFile.setVolume(1.0f);
        	
        	if (!mPlayingFile.isPlaying())
        		startPlaylist(mPlaylistString, mPlayingFile.getKey(), mPlayingFile.getCurrentPosition());
        	
            return;
		}
		
		if (mPlayingFile == null) return;
		
	    switch (focusChange) {
        	// Lost focus for an unbounded amount of time: stop playback and release media player
	        case AudioManager.AUDIOFOCUS_LOSS:
	        	if (mPlayingFile.isPlaying()) pausePlayback(true);
	        // Lost focus but it will be regained... could not release resources if wanted
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	        	if (mPlayingFile.isPlaying()) pausePlayback(false);
	            return;
	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (mPlayingFile.isPlaying()) mPlayingFile.setVolume(0.1f);
	            return;
	    }
	}
	
	@Override
	public void onDestroy() {
		JrSession.SaveSession(this);
		stopNotification();
		if (trackProgressThread != null && trackProgressThread.isAlive()) trackProgressThread.interrupt();
		releaseMediaPlayers();
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
