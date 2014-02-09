package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import ch.qos.logback.classic.Logger;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.bluewater.data.service.access.connection.JrTestConnection;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class JrFile extends JrObject implements
	OnPreparedListener, 
	OnErrorListener, 
	OnCompletionListener	
{
	private ConcurrentSkipListMap<String, String> mProperties = null;
	
	private boolean prepared = false;
	private boolean preparing = false;
	private int mPosition = 0;
	private float mVolume = 1.0f;
	private Context mMpContext;;
	private MediaPlayer mp;
	private LinkedList<OnJrFileCompleteListener> onJrFileCompleteListeners = new LinkedList<OnJrFileCompleteListener>();
	private LinkedList<OnJrFilePreparedListener> onJrFilePreparedListeners = new LinkedList<OnJrFilePreparedListener>();
	private LinkedList<OnJrFileErrorListener> onJrFileErrorListeners = new LinkedList<OnJrFileErrorListener>();
	private JrFile mNextFile, mPreviousFile;
	private static ExecutorService fileStatsExecutor = Executors.newSingleThreadExecutor();
	
	public JrFile(int key) {
		this();
		this.setKey(key);
	}
	
	public JrFile(int key, String value) {
		this();
		this.setKey(key);
		this.setValue(value);
	}
	
	public JrFile() {
		super();
		mProperties = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	}
	

	/**
	 * @return the Value
	 */
	@Override
	public String getValue() {
		if (super.getValue() == null) {
			try {
				setValue(getProperty("Name"));
			} catch (IOException e) {
				LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
			}
		}
		return super.getValue();
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
	
	public String getSubItemUrl() {
		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.; 
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
		return JrSession.accessDao.getJrUrl("File/GetFile", "File=" + Integer.toString(getKey()), "Quality=medium", "Conversion=Android", "Playback=0");
	}
	
	private String getMpUrl() {
		if (!JrTestConnection.doTest()) {
			for (OnJrFileErrorListener listener : onJrFileErrorListeners) listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
			return null;
		}
		return getSubItemUrl();
	}
	
	/**
	 * @return the prepared
	 */
	public boolean isPrepared() {
		return prepared;
	}
		
	public JrFile getNextFile() {
		return mNextFile;
	}
	
	public void setNextFile(JrFile file) {
		mNextFile = file;
	}
	
	public JrFile getPreviousFile() {
		return mPreviousFile;
	}
	
	public void setPreviousFile(JrFile file) {
		mPreviousFile = file;
	}
		
	public void setProperty(String name, String value) {
		if (mProperties.containsKey(name) && mProperties.get(name).equals(value)) return;

		AsyncTask<String, Void, Boolean> setPropertyTask = new AsyncTask<String, Void, Boolean>() {
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					JrConnection conn = new JrConnection("File/SetInfo", "File=" + params[0], "Field=" + params[1], "Value=" + params[2]);
					conn.setReadTimeout(5000);
					conn.getInputStream();
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		};
		setPropertyTask.executeOnExecutor(fileStatsExecutor, String.valueOf(getKey()), name, value);
		
		mProperties.put(name, value);
	}
	
	public String getProperty(String name) throws IOException {
		
		if (mProperties.size() == 0 || !mProperties.containsKey(name))
			return getRefreshedProperty(name);
		
		return mProperties.get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		String result = null;
		
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		SimpleTask<String, Void, Map<String,String>> filePropertiesTask = new SimpleTask<String, Void, Map<String,String>>();
		filePropertiesTask.addOnExecuteListener(new OnExecuteListener<String, Void, Map<String,String>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, Map<String, String>> owner, String... params) throws IOException {
				HashMap<String, String> returnProperties = new HashMap<String, String>();

				try {
					JrConnection conn = new JrConnection("File/GetInfo", "File=" + String.valueOf(getKey()));
					conn.setReadTimeout(45000);
					try {
				    	XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
				    	if (xml.size() < 1) return;
				    	returnProperties = new HashMap<String, String>(xml.get(0).size());
				    	for (XmlElement el : xml.get(0))
				    		returnProperties.put(el.getAttribute("Name"), el.getValue());
					} finally {
						conn.disconnect();
					}
				} catch (MalformedURLException e) {
					LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
				} catch (XmlParseException e) {
					LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
				}
				
				owner.setResult(returnProperties);
			}
		});
		
		filePropertiesTask.addOnErrorListener(new com.lasthopesoftware.threading.ISimpleTask.OnErrorListener<String, Void, Map<String,String>>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, Map<String, String>> owner, Exception innerException) {
				return !(innerException instanceof IOException);
			}
		});

		try {
			Map<String, String> filePropertiesResult = filePropertiesTask.executeOnExecutor(fileStatsExecutor).get();
			
			if (filePropertiesTask.getState() == SimpleTaskState.ERROR) {
				for (Exception e : filePropertiesTask.getExceptions()) {
					if (e instanceof IOException) throw (IOException)e;
				}
			}
			
			if (filePropertiesResult == null) return mProperties.containsKey(name) ? mProperties.get(name) : null;
			
			if (filePropertiesResult.containsKey(name))
				result = filePropertiesResult.get(name);
			
			mProperties.putAll(filePropertiesResult);
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
		}
		
		return result;
	}
	
	public void initMediaPlayer(Context context) {
		if (mp != null) return;
		
		this.mMpContext = context;
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mp != null;
	}
	
	public void prepareMediaPlayer() {
		if (!preparing && !prepared) {
			try {
				String url = getMpUrl();
				if (!url.isEmpty()) {
					setMpDataSource(url);
					preparing = true;
					mp.prepareAsync();
					return;
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(JrFile.class).error(e.toString(), e);
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
		Thread updateStatsThread = new Thread(new UpdatePlayStatsRunner(this));
		updateStatsThread.setName("Asynchronous Update Stats Thread for " + getValue());
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
		if (mp == null || !isPrepared()) {
			String durationToParse = getProperty("Duration");
			if (durationToParse != null && !durationToParse.isEmpty())
				return (int) (Double.parseDouble(durationToParse) * 1000);
			throw new IOException("Duration was not present in the song properties.");
		}
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
