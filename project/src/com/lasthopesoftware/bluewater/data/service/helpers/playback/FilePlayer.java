package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.MediaStore;

import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class FilePlayer implements
	OnPreparedListener,
	OnErrorListener, 
	OnCompletionListener,
	OnBufferingUpdateListener
{
	private static final Logger mLogger = LoggerFactory.getLogger(FilePlayer.class);
	
	private volatile MediaPlayer mp;
	private final AtomicBoolean isPrepared = new AtomicBoolean();
	private final AtomicBoolean isPreparing = new AtomicBoolean();
	private final AtomicBoolean isInErrorState = new AtomicBoolean();
	private volatile int mPosition = 0;
	private volatile int mBufferPercentage = 0;
	private volatile float mVolume = 1.0f;
	private final Context mMpContext;
	private final File mFile;
	
	private static final String FILE_URI_SCHEME = "file://";
	private static final String MEDIA_DATA_QUERY = 	MediaStore.Audio.Media.DATA + " LIKE '%' || ? || '%' ";
	
	private static final String[] MEDIA_QUERY_PROJECTION = { MediaStore.Audio.Media.DATA };
	
	private final HashSet<OnFileCompleteListener> onFileCompleteListeners = new HashSet<OnFileCompleteListener>();
	private final HashSet<OnFilePreparedListener> onFilePreparedListeners = new HashSet<OnFilePreparedListener>();
	private final HashSet<OnFileErrorListener> onFileErrorListeners = new HashSet<OnFileErrorListener>();
	private final HashSet<OnFileBufferedListener> onFileBufferedListeners = new HashSet<OnFileBufferedListener>();
	
	public FilePlayer(Context context, File file) {
		mMpContext = context;
		mFile = file;
	}
	
	public File getFile() {
		return mFile;
	}
	
	public void initMediaPlayer() {
		if (mp != null) return;
		
		isPrepared.set(false);
		isPreparing.set(false);
		isInErrorState.set(false);
		mBufferPercentage = 0;
		
		mp = new MediaPlayer(); // initialize it here
		mp.setOnPreparedListener(this);
		mp.setOnErrorListener(this);
		mp.setOnCompletionListener(this);
		mp.setOnBufferingUpdateListener(this);
		mp.setWakeMode(mMpContext, PowerManager.PARTIAL_WAKE_LOCK);
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
	}
	
	public boolean isMediaPlayerCreated() {
		return mp != null;
	}
	
	public boolean isPrepared() {
		return isPrepared.get();
	}
	
	@SuppressLint("InlinedApi")
	private Uri getMpUri() throws IOException {
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");
				
		final String originalFilename = mFile.getProperty(FileProperties.FILENAME);
		if (originalFilename == null)
			throw new IOException("The filename property was not retrieved. A connection needs to be re-established.");
		
		final String filename = originalFilename.substring(originalFilename.lastIndexOf('\\') + 1, originalFilename.lastIndexOf('.'));
		
		final StringBuilder querySb = new StringBuilder(MEDIA_DATA_QUERY);
		appendAnd(querySb);
		
		final ArrayList<String> params = new ArrayList<String>(5);
		params.add(filename);
		
		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ARTIST, mFile.getProperty(FileProperties.ARTIST));
		appendAnd(querySb);
		
		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.ALBUM, mFile.getProperty(FileProperties.ALBUM));
		appendAnd(querySb);
		
		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TITLE, mFile.getProperty(FileProperties.NAME));
		appendAnd(querySb);
		
		appendPropertyFilter(querySb, params, MediaStore.Audio.Media.TRACK, mFile.getProperty(FileProperties.TRACK));
		
		final Cursor cursor = mMpContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MEDIA_QUERY_PROJECTION, querySb.toString(), params.toArray(new String[params.size()]), null);
	    try {
		    if (cursor.moveToFirst()) {
		    	final String fileUriString = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
		    	if (fileUriString != null && !fileUriString.isEmpty()) {
		    		// The file object will produce a properly escaped File URI, as opposed to what is stored in the DB
		    		final java.io.File file = new java.io.File(fileUriString.replaceFirst(FILE_URI_SCHEME, ""));
		    		
		    		if (file != null && file.exists()) {
		    			mLogger.info("Returning file URI from local disk.");
		    			return Uri.fromFile(file);
		    		}
		    	}
		    }
	    } catch (IllegalArgumentException ie) {
	    	mLogger.info("Illegal column name.", ie);
	    } finally {
	    	cursor.close();
	    }
	    
	    mLogger.info("Returning file URL from server.");
	    final String itemUrl = mFile.getSubItemUrl();
	    if (itemUrl != null && !itemUrl.isEmpty())
	    	return Uri.parse(itemUrl);
	    
	    return null;
	}
	
	private static StringBuilder appendPropertyFilter(final StringBuilder querySb, final ArrayList<String> params, final String key, final String value) {
		querySb.append(' ').append(key).append(' ');
		
		if (value != null) {
			querySb.append(" = ? ");
			params.add(value);
		} else {
			querySb.append(" IS NULL ");
		}
		
		return querySb;
	}
	
	private final static StringBuilder appendAnd(final StringBuilder querySb) {
		return querySb.append(" AND ");
	}
	
	public void prepareMediaPlayer() {
		if (isPreparing.get() || isPrepared.get()) return;
		
		try {
			final Uri uri = getMpUri();
			if (uri != null) {
				setMpDataSource(uri);
				isPreparing.set(true);
				mLogger.info("Preparing " + mFile.getValue() + " asynchronously.");
				mp.prepareAsync();
			}
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing.set(false);
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing.set(false);
		}
	}
	
	public void prepareMpSynchronously() {
		if (isPreparing.get() || isPrepared.get()) return;
		
		try {
			final Uri uri = getMpUri();
			if (uri != null) {
				setMpDataSource(uri);
				
				isPreparing.set(true);
				mLogger.info("Preparing " + mFile.getValue() + " synchronously.");
				mp.prepare();
				isPrepared.set(true);
				return;
			}
			
			isPreparing.set(false);
		} catch (IOException io) {
			throwIoErrorEvent();
			isPreparing.set(false);
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			resetMediaPlayer();
			isPreparing.set(false);
		}
	}
	
	private void throwIoErrorEvent() {
		isInErrorState.set(true);
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners)
			listener.onJrFileError(this, MediaPlayer.MEDIA_ERROR_SERVER_DIED, MediaPlayer.MEDIA_ERROR_IO);
	}
	
	private void setMpDataSource(Uri uri) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException {
		final Map<String, String> headers = new HashMap<String, String>();
		if (mMpContext == null)
			throw new NullPointerException("The file player's context cannot be null");
		
		final String authKey = LibrarySession.GetLibrary(mMpContext).getAuthKey();
		if (authKey != null && !authKey.isEmpty())
			headers.put("Authorization", "basic " + authKey);
		
		mp.setDataSource(mMpContext, uri, headers);
	}
	
	private void resetMediaPlayer() {
		
		final int position = getCurrentPosition();
		releaseMediaPlayer();
		
		initMediaPlayer();
		
		if (position > 0)
			seekTo(position);
	}
	
	public void releaseMediaPlayer() {
		if (mp != null) mp.release();
		mp = null;
		isPrepared.set(false);
	}
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		isPrepared.set(true);
		isPreparing.set(false);
		mLogger.info(mFile.getValue() + " prepared!");
		for (OnFilePreparedListener listener : onFilePreparedListeners) listener.onJrFilePrepared(this);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					final String lastPlayedString = mFile.getProperty(FileProperties.LAST_PLAYED);
					// Only update the last played data if the song could have actually played again
					if (lastPlayedString == null || (System.currentTimeMillis() - getDuration()) > Long.valueOf(lastPlayedString)) {
						final SimpleTask<Void, Void, Void> updateStatsTask = new SimpleTask<Void, Void, Void>(new UpdatePlayStatsOnExecute(mFile));
						updateStatsTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				} catch (NumberFormatException e) {
					mLogger.error("There was an error parsing the last played time.");
				} catch (IOException e) {
					mLogger.warn("There was an error retrieving the duration or last played time data.");
				}
			}
		});
		
		releaseMediaPlayer();
		for (OnFileCompleteListener listener : onFileCompleteListeners) listener.onJrFileComplete(this);
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.setOnErrorListener(null);
		isInErrorState.set(true);
		mLogger.error("Media Player error.");
		mLogger.error("What: ");
		mLogger.error(what == MediaPlayer.MEDIA_ERROR_UNKNOWN ? "MEDIA_ERROR_UNKNOWN" : "MEDIA_ERROR_SERVER_DIED");
		mLogger.error("Extra: ");
		switch (extra) {
			case MediaPlayer.MEDIA_ERROR_IO:
				mLogger.error("MEDIA_ERROR_IO");
				break;
			case MediaPlayer.MEDIA_ERROR_MALFORMED:
				mLogger.error("MEDIA_ERROR_MALFORMED");
				break;
			case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
				mLogger.error("MEDIA_ERROR_UNSUPPORTED");
				break;
			case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
				mLogger.error("MEDIA_ERROR_TIMED_OUT");
				break;
			case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
				mLogger.error("MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
				break;
			default:
				mLogger.error("Unknown");
				break;
		}
		resetMediaPlayer();
		
		for (OnFileErrorListener listener : onFileErrorListeners) listener.onJrFileError(this, what, extra);
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// Handle weird exceptional behavior seen online http://stackoverflow.com/questions/21925454/android-mediaplayer-onbufferingupdatelistener-percentage-of-buffered-content-i
        if (percent < 0 || percent > 100) {
            mLogger.warn("Buffering percentage was bad: " + String.valueOf(percent));
            percent = (int) Math.round((((Math.abs(percent)-1)*100.0/Integer.MAX_VALUE)));
        }

		mBufferPercentage = percent;
		
		if (percent < 100) return;
		
		// remove the listener
		mp.setOnBufferingUpdateListener(null);
		
		for (OnFileBufferedListener onFileBufferedListener : onFileBufferedListeners)
			onFileBufferedListener.onFileBuffered(this);
	}
	
	public int getCurrentPosition() {
		try {
			if (mp != null && isPrepared()) mPosition = mp.getCurrentPosition();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
		return mPosition;
	}
	
	public int getBufferPercentage() {
		return mBufferPercentage;
	}
	
	public int getDuration() throws IOException {
		if (mp == null || isInErrorState.get() || !isPlaying())
			return mFile.getDuration();
		
		try {
			return mp.getDuration();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return mFile.getDuration();
		}
	}

	public boolean isPlaying() {
		try {
			return mp != null && mp.isPlaying();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
			return false;
		}
	}

	public void pause() {
		if (isPreparing.get()) {
			try {
				mp.reset();
			} catch (IllegalStateException e) {
				handleIllegalStateException(e);
				resetMediaPlayer();
				return;
			}
		}
		
		try {
			mPosition = mp.getCurrentPosition();
			mp.pause();
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void seekTo(int pos) {
		mPosition = pos;
		try {
			if (mp != null && !isInErrorState.get() && isPrepared() && isPlaying()) mp.seekTo(mPosition);
		} catch (IllegalStateException ie) {
			handleIllegalStateException(ie);
		}
	}

	public void start() throws IllegalStateException {
		mLogger.info("Playback started on " + mFile.getValue());
		mp.seekTo(mPosition);
		mp.start();
	}
	
	public void stop() throws IllegalStateException {
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
	
	private static void handleIllegalStateException(IllegalStateException ise) {
		mLogger.warn("The media player was in an incorrect state.", ise);
	}
	
	private static class UpdatePlayStatsOnExecute implements OnExecuteListener<Void, Void, Void> {
		private final File mFile;
		
		public UpdatePlayStatsOnExecute(File file) {
			mFile = file;
		}
		
		@Override
		public Void onExecute(ISimpleTask<Void, Void, Void> owner, Void... params) throws Exception {
			try {
				final String numberPlaysString = mFile.getRefreshedProperty("Number Plays");
				
				int numberPlays = 0;
				if (numberPlaysString != null && !numberPlaysString.isEmpty()) numberPlays = Integer.parseInt(numberPlaysString);
				
				mFile.setProperty(FileProperties.NUMBER_PLAYS, String.valueOf(++numberPlays));	
				
				final String lastPlayed = String.valueOf(System.currentTimeMillis()/1000);
				mFile.setProperty(FileProperties.LAST_PLAYED, lastPlayed);
			} catch (IOException e) {
				mLogger.warn(e.toString(), e);
			} catch (NumberFormatException ne) {
				mLogger.error(ne.toString(), ne);
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
