package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;

import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.servers.repository.Library;
import com.lasthopesoftware.bluewater.shared.IoCommon;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlaybackFile implements
	IPlaybackFile,
	OnPreparedListener,
	OnErrorListener, 
	OnCompletionListener,
	OnBufferingUpdateListener
{
	@SuppressLint("InlinedApi")
	public static final Set<Integer> MEDIA_ERROR_EXTRAS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[] {
		MediaPlayer.MEDIA_ERROR_IO,
		MediaPlayer.MEDIA_ERROR_MALFORMED,
		MediaPlayer.MEDIA_ERROR_UNSUPPORTED, 
		MediaPlayer.MEDIA_ERROR_TIMED_OUT, 
		MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
	}))); 
	
	private static final Logger logger = LoggerFactory.getLogger(PlaybackFile.class);
	
	private volatile MediaPlayer mediaPlayer;
	
	// FilePlayer State Variables
	private volatile boolean isPrepared = false;
	private volatile boolean isPreparing = false;
	private volatile boolean isInErrorState = false;
	private volatile int position = 0;
	private volatile int bufferPercentage = 0;
	private volatile int lastBufferPercentage = 0;
	private volatile float volume = 1.0f;
	
	private final Context mpContext;
	private final IFile file;
	private final ConnectionProvider connectionProvider;
	
	private static final int mBufferMin = 0, mBufferMax = 100;

	private final HashSet<OnFileCompleteListener> onFileCompleteListeners = new HashSet<>();
	private final HashSet<OnFilePreparedListener> onFilePreparedListeners = new HashSet<>();
	private final HashSet<OnFileErrorListener> onFileErrorListeners = new HashSet<>();
	private final HashSet<OnFileBufferedListener> onFileBufferedListeners = new HashSet<>();
	
	public PlaybackFile(Context context, ConnectionProvider connectionProvider, IFile file) {
		mpContext = context;
		this.connectionProvider = connectionProvider;
		this.file = file;
	}
	
	public IFile getFile() {
		return file;
	}
	
	public void initMediaPlayer() {
		if (mediaPlayer != null) return;
		
		isPrepared = false;
		isPreparing = false;
		isInErrorState = false;
		bufferPercentage = mBufferMin;
		
		mediaPlayer = new MediaPlayer(); // initialize it here
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setWakeMode(mpContext, PowerManager.PARTIAL_WAKE_LOCK);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mediaPlayer != null;
	}
	
	public boolean isPrepared() {
		return isPrepared;
	}

	private Uri getFileUri() throws IOException {
		final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(mpContext, connectionProvider, LibrarySession.GetActiveLibrary(mpContext), file);
		return bestMatchUriProvider.getFileUri();
	}

	public void prepareMediaPlayer() {
		if (isPreparing || isPrepared) return;
		
		try {
			final Uri uri = getFileUri();
			if (uri == null) return;
			
			setMpDataSource(uri);
			initializeBufferPercentage(uri);
			
			isPreparing = true;
			
			logger.info("Preparing " + file.getValue() + " asynchronously.");
			mediaPlayer.prepareAsync();
		} catch (FileNotFoundException fe) {
			logger.error(fe.toString(), fe);
			resetMediaPlayer();
			isPreparing = false;
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing = false;
		} catch (Exception e) {
			logger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing = false;
		}
	}
	
	public void prepareMpSynchronously() {
		if (isPreparing || isPrepared) return;
		
		try {
			final Uri uri = getFileUri();
			if (uri == null) return;
			
			setMpDataSource(uri);
			initializeBufferPercentage(uri);
			
			isPreparing = true;
			
			logger.info("Preparing " + file.getValue() + " synchronously.");
			mediaPlayer.prepare();
			
			isPrepared = true;
			isPreparing = false;
		} catch (FileNotFoundException fe) {
			logger.error(fe.toString(), fe);
			resetMediaPlayer();
			isPreparing = false;
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing = false;
		} catch (Exception e) {
			logger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing = false;
		}
	}
	
	private void initializeBufferPercentage(Uri uri) {
		final String scheme = uri.getScheme();
		bufferPercentage = IoCommon.FileUriScheme.equalsIgnoreCase(scheme) ? mBufferMax : mBufferMin;
		logger.info("Initialized " + scheme + " type URI buffer percentage to " + String.valueOf(bufferPercentage));
	}
	
	private void throwIoErrorEvent() {
		isInErrorState = true;
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<>();
		if (mpContext == null)
			throw new NullPointerException("The file player's context cannot be null");

		if (!uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)) {
			final Library library = LibrarySession.GetActiveLibrary(mpContext);
			if (library != null) {
				final String authKey = library.getAuthKey();

				if (authKey != null && !authKey.isEmpty())
					headers.put("Authorization", "basic " + authKey);
			}
		}
		
		mediaPlayer.setDataSource(mpContext, uri, headers);
	}
	
	private void resetMediaPlayer() {
		final int position = getCurrentPosition();
		releaseMediaPlayer();
		
		initMediaPlayer();
		
		if (position > 0) seekTo(position);
	}
	
	public void releaseMediaPlayer() {
		if (mediaPlayer != null) mediaPlayer.release();
		mediaPlayer = null;
		isPrepared = false;
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		isPrepared = true;
		isPreparing = false;
		logger.info(file.getValue() + " prepared!");
		
		for (OnFilePreparedListener listener : onFilePreparedListeners) listener.onFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					final String lastPlayedString = file.getProperty(FilePropertiesProvider.LAST_PLAYED);
					// Only update the last played data if the song could have actually played again
					if (lastPlayedString == null || (System.currentTimeMillis() - getDuration()) > Long.valueOf(lastPlayedString))
						SimpleTask.executeNew(AsyncTask.THREAD_POOL_EXECUTOR, new UpdatePlayStatsOnExecute(file));
				} catch (NumberFormatException e) {
					logger.error("There was an error parsing the last played time.");
				} catch (IOException e) {
					logger.warn("There was an error retrieving the duration or last played time data.");
				}
			}
		});
		
		releaseMediaPlayer();
		for (OnFileCompleteListener listener : onFileCompleteListeners) listener.onFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.setOnErrorListener(null);
		isInErrorState = true;
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
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
				logger.error("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
				break;
			default:
				logger.error("Unknown");
				break;
		}
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners) listener.onFileError(this, what, extra);
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// Handle weird exceptional behavior seen online http://stackoverflow.com/questions/21925454/android-mediaplayer-onbufferingupdatelistener-percentage-of-buffered-content-i
        if (percent < 0 || percent > 100) {
            logger.warn("Buffering percentage was bad: " + String.valueOf(percent));
            percent = (int) Math.round((((Math.abs(percent)-1)*100.0/Integer.MAX_VALUE)));
        }
        
		bufferPercentage = percent;
		
		if (!isBuffered()) return;
		
		// remove the listener
		mp.setOnBufferingUpdateListener(null);
		
		for (OnFileBufferedListener onFileBufferedListener : onFileBufferedListeners)
			onFileBufferedListener.onFileBuffered(this);
	}
	
	public int getCurrentPosition() {
		try {
			if (mediaPlayer != null && isPrepared()) position = mediaPlayer.getCurrentPosition();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
		return position;
	}
	
	public boolean isBuffered() {
		if (lastBufferPercentage != bufferPercentage) {
			lastBufferPercentage = bufferPercentage;
			logger.info("Buffer percentage: " + String.valueOf(bufferPercentage) + "% Buffer Threshold: " + String.valueOf(mBufferMax) + "%");
		}
		return bufferPercentage >= mBufferMax;
	}
	
	public int getBufferPercentage() {
		return bufferPercentage;
	}
	
	public int getDuration() throws IOException {
		if (mediaPlayer == null || isInErrorState || !isPlaying())
			return file.getDuration();
		
		try {
			return mediaPlayer.getDuration();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return file.getDuration();
		}
	}

	public boolean isPlaying() {
		try {
			return mediaPlayer != null && mediaPlayer.isPlaying();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return false;
		}
	}

	public void pause() {
		if (isPreparing) {
			try {
				mediaPlayer.reset();
			} catch (IllegalStateException e) {
				handleIllegalStateException(e);
				resetMediaPlayer();
				return;
			}
		}
		
		try {
			position = mediaPlayer.getCurrentPosition();
			mediaPlayer.pause();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void seekTo(int pos) {
		position = pos;
		try {
			if (mediaPlayer != null && !isInErrorState && isPrepared() && isPlaying()) mediaPlayer.seekTo(position);
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void start() throws IllegalStateException {
		logger.info("Playback started on " + file.getValue());
		mediaPlayer.seekTo(position);
		mediaPlayer.start();
	}
	
	public void stop() throws IllegalStateException {
		position = 0;
		mediaPlayer.stop();
	}
	
	public float getVolume() {
		return volume;
	}
	
	public void setVolume(float volume) {
		this.volume = volume;
		
		if (mediaPlayer != null)
			mediaPlayer.setVolume(this.volume, this.volume);
	}
	
	private static void handleIllegalStateException(IllegalStateException ise) {
		logger.warn("The media player was in an incorrect state.", ise);
	}
	
	private static class UpdatePlayStatsOnExecute implements OnExecuteListener<Void, Void, Void> {
		private final IFile mFile;
		
		public UpdatePlayStatsOnExecute(IFile file) {
			mFile = file;
		}
		
		@Override
		public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
			try {
				final String numberPlaysString = mFile.getRefreshedProperty(FilePropertiesProvider.NUMBER_PLAYS);
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty(FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays));
				
				final String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
				mFile.setProperty(FilePropertiesProvider.LAST_PLAYED, lastPlayed);
			} catch (IOException e) {
				logger.warn(e.toString(), e);
			} catch (NumberFormatException ne) {
				logger.error(ne.toString(), ne);
			}
			
			return null;
		}
	}
	
	/* Listener methods */
	public void addOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListeners.add(listener);
	}
	
	public void removeOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListeners.remove(listener);
	}
	
	public void addOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListeners.add(listener);
	}
	
	public void removeOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListeners.remove(listener);
	}
	
	public void addOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.add(listener);
	}
	
	public void removeOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.remove(listener);
	}
	
	public void addOnFileBufferedListener(OnFileBufferedListener listener) {
		onFileBufferedListeners.add(listener);
	}
	
	public void removeOnFileErrorListener(OnFileBufferedListener listener) {
		onFileBufferedListeners.remove(listener);
	}
}
