package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrTestConnection;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFilePreparedListener;
import com.lasthopesoftware.bluewater.data.session.JrSession;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;

public class JrFilePlayer implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener
{
	private MediaPlayer mp;
	private boolean prepared = false;
	private boolean preparing = false;
	private int mPosition = 0;
	private float mVolume = 1.0f;
	private Context mMpContext;
	private JrFile mFile;
	
	private LinkedList<OnJrFileCompleteListener> onJrFileCompleteListeners = new LinkedList<OnJrFileCompleteListener>();
	private LinkedList<OnJrFilePreparedListener> onJrFilePreparedListeners = new LinkedList<OnJrFilePreparedListener>();
	private LinkedList<OnJrFileErrorListener> onJrFileErrorListeners = new LinkedList<OnJrFileErrorListener>();
	
	public JrFilePlayer(Context context, JrFile file) {
		mMpContext = context;
		mFile = file;
	}
	
	public void addOnJrFileCompleteListener(OnJrFileCompleteListener listener) {
		onJrFileCompleteListeners.add(listener);
	}
	
	public void removeOnJrFileCompleteListener(OnJrFileCompleteListener listener) {
		if (onJrFileCompleteListeners.contains(listener)) onJrFileCompleteListeners.remove(listener);
	}
	
	public void addOnJrFilePreparedListener(OnJrFilePreparedListener listener) {
		onJrFilePreparedListeners.add(listener);
	}
	
	public void removeOnJrFilePreparedListener(OnJrFilePreparedListener listener) {
		if (onJrFilePreparedListeners.contains(listener)) onJrFilePreparedListeners.remove(listener);
	}
	
	public void addOnJrFileErrorListener(OnJrFileErrorListener listener) {
		onJrFileErrorListeners.add(listener);
	}
	
	public void removeOnJrFileErrorListener(OnJrFileErrorListener listener) {
		if (onJrFileErrorListeners.contains(listener)) onJrFileErrorListeners.remove(listener);
	}
	
	public JrFile getFile() {
		return mFile;
	}
	
	public void initMediaPlayer() {
		if (mp != null) return;
		
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mp != null;
	}
	
	public boolean isPrepared() {
		return prepared;
	}
	
	private String getMpUrl() {
		if (!JrTestConnection.doTest()) {
			for (OnJrFileErrorListener listener : onJrFileErrorListeners) listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
			return null;
		}
		return mFile.getSubItemUrl();
	}
	
	public void prepareMediaPlayer() {
		if (!preparing && !prepared) {
			try {
//				String[] proj = { MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA };
//				String fileName = mFile.getProperty("Filename").substring(mFile.getProperty("Filename").lastIndexOf('\\') + 1);
//				CursorLoader loader = new CursorLoader(mMpContext, MediaStore.Audio.Media.INTERNAL_CONTENT_URI, proj, MediaStore.Audio.Media.TITLE + " = ?", new String[] { mFile.getValue() }, null);
//			    Cursor cursor = loader.loadInBackground();
//			    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//			    if (cursor.getCount() > 0) {
//				    cursor.moveToFirst();
//				    String fileUri = cursor.getString(column_index);
//				    LoggerFactory.getLogger(getClass()).debug("File URI: " + fileUri);
//			    }
			    
//				File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
//				for (File file : directory.listFiles()) {
//					LoggerFactory.getLogger(getClass()).debug("File path: " + file.getAbsolutePath());
//				}
				String uri = getMpUrl();
				if (!uri.isEmpty()) {
					setMpDataSource(uri);
					preparing = true;
					mp.prepareAsync();
					return;
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(JrFilePlayer.class).error(e.toString(), e);
			}
		}
	}
	
	public void prepareMpSynchronously() {
		if (!preparing && !prepared) {
			try {
				String url = getMpUrl();
				if (!url.isEmpty()) {
					setMpDataSource(url);
					
					preparing = true;
					mp.prepare();
					prepared = true;
					return;
				}
				
				preparing = false;
			} catch (Exception e) {
				LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
				resetMediaPlayer();
				preparing = false;
			}
		}
	}
	
	private void setMpDataSource(String url) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		Map<String, String> headers = new HashMap<String, String>();
		if (!JrSession.GetLibrary(mMpContext).getAuthKey().isEmpty())
			headers.put("Authorization", "basic " + JrSession.GetLibrary(mMpContext).getAuthKey());
		mp.setDataSource(mMpContext, Uri.parse(url), headers);
	}
	
	private void resetMediaPlayer() {
		if (mp == null) {
			initMediaPlayer();
			return;
		}
		
		mp.reset();
		
		if (mMpContext != null)
			mp.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public void releaseMediaPlayer() {
		if (mp != null) mp.release();
		mp = null;
		prepared = false;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		prepared = true;
		preparing = false;
		for (OnJrFilePreparedListener listener : onJrFilePreparedListeners) listener.onJrFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		Thread updateStatsThread = new Thread(new UpdatePlayStatsRunner(mFile));
		updateStatsThread.setName("Asynchronous Update Stats Thread for " + mFile.getValue());
		updateStatsThread.setPriority(Thread.MIN_PRIORITY);
		updateStatsThread.start();
		
		releaseMediaPlayer();
		for (OnJrFileCompleteListener listener : onJrFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Logger logger = (Logger) LoggerFactory.getLogger(JrFile.class);
		logger.error("Media Player error.");
		logger.error("What: ");
		logger.error(what == MediaPlayer.MEDIA_ERROR_UNKNOWN ? "MEDIA_ERROR_UNKNOWN" : "MEDIA_ERROR_SERVER_DIED");
		logger.error("Extra: ");
		switch (extra) {
		case MediaPlayer.MEDIA_ERROR_IO:
			logger.error("MEDIA_ERROR_IO");
			break;
		case MediaPlayer.MEDIA_ERROR_MALFORMED:
			logger.error("MEDIA_ERROR_MALFORMED");
			break;
		case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
			logger.error("MEDIA_ERROR_UNSUPPORTED");
			break;
		case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
			logger.error("MEDIA_ERROR_TIMED_OUT");
			break;
		}
		resetMediaPlayer();
		boolean handled = false;
		for (OnJrFileErrorListener listener : onJrFileErrorListeners) handled |= listener.onJrFileError(this, what, extra);
		if (handled) releaseMediaPlayer();
		return handled;
	}

	public int getBufferPercentage() {
		return (mp.getCurrentPosition() * 100) / mp.getDuration();
	}

	public int getCurrentPosition() {
		if (mp != null && isPrepared() && isPlaying()) mPosition = mp.getCurrentPosition();
		return mPosition;
	}
	
	public int getDuration() throws IOException {
		if (mp == null || !isPrepared())
			return mFile.getDuration();
		
		return mp.getDuration();
	}

	public boolean isPlaying() {
		return mp != null && mp.isPlaying();
	}

	public void pause() {
		mPosition = mp.getCurrentPosition();
		mp.pause();
	}

	public void seekTo(int pos) {
		mPosition = pos;
		if (mp != null && isPrepared() && isPlaying()) mp.seekTo(mPosition);
	}

	public void start() {
		mp.seekTo(mPosition);
//		mp.setVolume(mVolume, mVolume);
		mp.start();
	}
	
	public void stop() {
		mPosition = 0;
		mp.stop();
	}
	
	public float getVolume() {
		return mVolume;
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		if (mp != null)
			mp.setVolume(mVolume, mVolume);
	}
	
	private static class UpdatePlayStatsRunner implements Runnable {
		private JrFile mFile;
		
		public UpdatePlayStatsRunner(JrFile file) {
			mFile = file;
		}
		
		@Override
		public void run() {
			try {
				String numberPlaysString = mFile.getRefreshedProperty("Number Plays");
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty("Number Plays", String.valueOf(++numberPlays));				
			} catch (IOException e) {
				LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
			} catch (NumberFormatException ne) {
				LoggerFactory.getLogger(JrFile.class).error(ne.toString(), ne);
			}
			
			String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
			mFile.setProperty("Last Played", lastPlayed);
		}
	}
}
