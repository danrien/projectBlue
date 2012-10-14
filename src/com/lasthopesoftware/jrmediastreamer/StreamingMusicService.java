/**
 * 
 */
package com.lasthopesoftware.jrmediastreamer;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * @author david
 *
 */
public class StreamingMusicService extends Service implements
		OnPreparedListener, 
		OnErrorListener, 
		OnCompletionListener,
		OnAudioFocusChangeListener
{
	//private final IBinder mBinder = 
	private static final String ACTION_PLAY = "com.example.action.PLAY";
	private int mId = 1;
	private MediaPlayer mMediaPlayer = null;
	private WifiLock mWifiLock = null;
	private String mUrl;
	private Notification mNotification;
		
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(ACTION_PLAY)) {
           initMediaPlayer();  
        }
		return START_STICKY;
	}
//	
//	public int onStopCommand() {
//		
//	}
	
	private void initMediaPlayer() {
		mMediaPlayer = new MediaPlayer(); // initialize it here
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "svcLock");
        mWifiLock.acquire();
        try {
        	mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setDataSource(mUrl);
			mMediaPlayer.prepareAsync(); // prepare async to not block main thread
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), StreamMedia.class), PendingIntent.FLAG_UPDATE_CURRENT);
        
        mNotification = new Notification();
//        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_launcher;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), 
        		"Music Streamer for J. River Media Center", 
        		"Playing",
        		pi);
        startForeground(mId, mNotification);
	}
	
	private void releaseMediaPlayer() {
		stopForeground(true);
		mWifiLock.release();
		mWifiLock = null;
		mMediaPlayer.release();
		mMediaPlayer = null;
	}

	/* Begin Event Handlers */
	
	/* (non-Javadoc)
	 * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		mMediaPlayer.start();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
//		return mBinder;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		mMediaPlayer.reset();
		return false;
	}


	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		releaseMediaPlayer();
	}


	@Override
	public void onAudioFocusChange(int focusChange) {
	    switch (focusChange) {
	        case AudioManager.AUDIOFOCUS_GAIN:
	            // resume playback
	            if (mMediaPlayer == null) initMediaPlayer();
	            else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
	            mMediaPlayer.setVolume(1.0f, 1.0f);
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS:
	            // Lost focus for an unbounded amount of time: stop playback and release media player
	            if (mMediaPlayer.isPlaying()) releaseMediaPlayer();
	            mMediaPlayer = null;
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	            // Lost focus for a short time, but we have to stop
	            // playback. We don't release the media player because playback
	            // is likely to resume
	            if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
	            break;

	        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	            // Lost focus for a short time, but it's ok to keep playing
	            // at an attenuated level
	            if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
	            break;
	    }
	}
	
	@Override
	public void onDestroy() {
		releaseMediaPlayer();
	}

	/* End Event Handlers */

}
