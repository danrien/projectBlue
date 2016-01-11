package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying;

import android.os.AsyncTask;
import android.os.Message;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NowPlayingActivityProgressTrackerTask extends AsyncTask<Void, Void, Void> {
	private final IPlaybackFile mFilePlayer;
	private final NowPlayingActivityMessageHandler mHandler;
	
	private static final ExecutorService mTrackerExecutor = Executors.newSingleThreadExecutor();
	
	public static NowPlayingActivityProgressTrackerTask trackProgress(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		final NowPlayingActivityProgressTrackerTask newProgressTrackerThread = new NowPlayingActivityProgressTrackerTask(filePlayer, handler);
		newProgressTrackerThread.executeOnExecutor(mTrackerExecutor);
		return newProgressTrackerThread;
	}
	
	private NowPlayingActivityProgressTrackerTask(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		mFilePlayer = filePlayer;
		mHandler = handler;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		while (!isCancelled() && mFilePlayer != null && mFilePlayer.isMediaPlayerCreated()) {
			try {
				
				if (mFilePlayer.isPlaying())
					mHandler.sendMessage(getUpdatePlayingMessage());
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
		
		return null;
	}
	
	private Message getUpdatePlayingMessage() {
		Message msg = new Message();
		msg.what = NowPlayingActivityMessageHandler.UPDATE_PLAYING;
		msg.arg1 = mFilePlayer.getCurrentPosition();
		try {
			msg.arg2 = mFilePlayer.getDuration();
		} catch (IOException e) {
			msg.what = NowPlayingActivityMessageHandler.SHOW_CONNECTION_LOST;
		}
		
		return msg;
	}
}