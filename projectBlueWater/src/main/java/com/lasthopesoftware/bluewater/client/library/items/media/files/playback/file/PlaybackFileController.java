package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

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
import android.support.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.IoCommon;

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
import java.util.concurrent.ExecutionException;

public class PlaybackFileController implements
	IPlaybackFile,
	OnPreparedListener,
	OnErrorListener, 
	OnCompletionListener,
	OnBufferingUpdateListener
{
	@SuppressLint("InlinedApi")
	public static final Set<Integer> MEDIA_ERROR_EXTRAS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[]{
			MediaPlayer.MEDIA_ERROR_IO,
			MediaPlayer.MEDIA_ERROR_MALFORMED,
			MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
			MediaPlayer.MEDIA_ERROR_TIMED_OUT,
			MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
	}))); 
	
	private static final Logger logger = LoggerFactory.getLogger(PlaybackFileController.class);
	
	private volatile MediaPlayer mediaPlayer;
	
	// FilePlayer State Variables
	private volatile boolean isPrepared = false;
	private volatile boolean isPreparing = false;
	private volatile boolean isInErrorState = false;
	private volatile int position = 0;
	private volatile int bufferPercentage = 0;
	private volatile int lastBufferPercentage = 0;
	private volatile float volume = 1.0f;
	private float maxVolume = 1.0f;

	private final Context context;
	private final IFile file;
	private final ConnectionProvider connectionProvider;
	
	private static final int bufferMin = 0, bufferMax = 100;

	private OnFileCompleteListener onFileCompleteListener;
	private OnFilePreparedListener onFilePreparedListener;
	private final HashSet<OnFileErrorListener> onFileErrorListeners = new HashSet<>();
	private OnFileBufferedListener onFileBufferedListener;

	public PlaybackFileController(@NonNull Context context, @NonNull ConnectionProvider connectionProvider, @NonNull IFile file) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.file = file;
	}

	@NonNull
	public IFile getFile() {
		return file;
	}
	
	public void initMediaPlayer() {
		if (mediaPlayer != null) return;
		
		isPrepared = false;
		isPreparing = false;
		isInErrorState = false;
		bufferPercentage = bufferMin;
		maxVolume = 1.0f;
		
		mediaPlayer = new MediaPlayer(); // initialize it here
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mediaPlayer != null;
	}
	
	public boolean isPrepared() {
		return isPrepared;
	}

	private Uri getFileUri() throws IOException {
		final BestMatchUriProvider bestMatchUriProvider = new BestMatchUriProvider(context, connectionProvider, LibrarySession.GetActiveLibrary(context), file);
		return bestMatchUriProvider.getFileUri();
	}

	public void prepareMediaPlayer() {
		AsyncTask.THREAD_POOL_EXECUTOR.execute(this::prepareMpSynchronously);
	}
	
	public void prepareMpSynchronously() {
		if (isPreparing || isPrepared) return;
		
		try {
			final Uri uri = getFileUri();
			if (uri == null) return;

			maxVolume = 1.0f;
			final MaxFileVolumeProvider maxFileVolumeProvider = new MaxFileVolumeProvider(context, connectionProvider, file);
			maxFileVolumeProvider.execute(AsyncTask.THREAD_POOL_EXECUTOR);

			setMpDataSource(uri);
			initializeBufferPercentage(uri);
			
			isPreparing = true;
			
			logger.info("Preparing " + file.getKey() + " synchronously.");
			mediaPlayer.prepare();

			try {
				maxVolume = maxFileVolumeProvider.get();
			} catch (ExecutionException | InterruptedException e) {
				logger.warn("There was an error getting the max file volume", e);
			}

			updateMediaPlayerVolume();
			
			isPrepared = true;
		} catch (FileNotFoundException fe) {
			logger.error(fe.toString(), fe);
			resetMediaPlayer();
		} catch (IOException io) {
			throwIoErrorEvent();
		} catch (Exception e) {
			logger.error(e.toString(), e);
			resetMediaPlayer();
		}

		isPreparing = false;
	}
	
	private void initializeBufferPercentage(Uri uri) {
		final String scheme = uri.getScheme();
		bufferPercentage = IoCommon.FileUriScheme.equalsIgnoreCase(scheme) ? bufferMax : bufferMin;
		logger.info("Initialized " + scheme + " type URI buffer percentage to " + String.valueOf(bufferPercentage));
	}
	
	private void throwIoErrorEvent() {
		isInErrorState = true;
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<>(1);

		if (!uri.getScheme().equalsIgnoreCase(IoCommon.FileUriScheme)) {
			final Library library = LibrarySession.GetActiveLibrary(context);
			if (library != null) {
				final String authKey = library.getAuthKey();

				if (authKey != null && !authKey.isEmpty())
					headers.put("Authorization", "basic " + authKey);
			}
		}
		
		mediaPlayer.setDataSource(context, uri, headers);
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
		logger.info(file.getKey() + " prepared!");

		if (onFilePreparedListener != null)
			onFilePreparedListener.onFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		releaseMediaPlayer();

		if (onFileCompleteListener != null)
			onFileCompleteListener.onFileComplete(this);
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
		
		if (onFileBufferedListener != null)
			onFileBufferedListener.onFileBuffered(this);
	}
	
	public int getCurrentPosition() {
		try {
			if (isPlaying()) position = mediaPlayer.getCurrentPosition();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
		return position;
	}
	
	public boolean isBuffered() {
		if (lastBufferPercentage != bufferPercentage) {
			lastBufferPercentage = bufferPercentage;
			logger.info("Buffer percentage: " + String.valueOf(bufferPercentage) + "% Buffer Threshold: " + String.valueOf(bufferMax) + "%");
		}
		return bufferPercentage >= bufferMax;
	}
	
	public int getBufferPercentage() {
		return bufferPercentage;
	}
	
	public int getDuration() {
		if (mediaPlayer == null || isInErrorState || !isPlaying())
			return -1;
		
		try {
			return mediaPlayer.getDuration();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return -1;
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
				return;
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
		logger.info("Playback started on " + file.getKey());
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
		
		updateMediaPlayerVolume();
	}

	private synchronized void updateMediaPlayerVolume() {
		if (mediaPlayer == null) return;

		final float leveledVolume = volume * maxVolume;
		mediaPlayer.setVolume(leveledVolume, leveledVolume);
	}
	
	private static void handleIllegalStateException(IllegalStateException ise) {
		logger.warn("The media player was in an incorrect state.", ise);
	}

	/* Listener methods */

	@Override
	public void setOnFileCompleteListener(OnFileCompleteListener listener) {
		onFileCompleteListener = listener;
	}

	@Override
	public IPlaybackFilePreparation setOnFilePreparedListener(OnFilePreparedListener listener) {
		onFilePreparedListener = listener;

		return this;
	}

	@Override
	public void setOnFileErrorListener(OnFileErrorListener listener) {
		onFileErrorListeners.add(listener);
	}

	@Override
	public void setOnFileBufferedListener(OnFileBufferedListener listener) {
		onFileBufferedListener = listener;
	}
}
