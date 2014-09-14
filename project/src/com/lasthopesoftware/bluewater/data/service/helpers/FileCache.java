package com.lasthopesoftware.bluewater.data.service.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Environment;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.CachedFile;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;

public class FileCache {
	
	private final static Logger mLogger = LoggerFactory.getLogger(FileCache.class); 
	
	private final Context mContext;
	private final Library mLibrary;
	private final String mCacheName;
	private final int mMaxSize;
	
	public FileCache(Context context, Library library, String cacheName, int maxSize) {
		mContext = context;
		mCacheName = cacheName;
		mMaxSize = maxSize;
		mLibrary = library;
	}
	
	public void put(final String uniqueKey, final File file) {
		final DatabaseHandler handler = new DatabaseHandler(mContext);
		try {
			CachedFile cachedFile = getCachedFile(handler, mLibrary.getId(), mCacheName, uniqueKey);
			if (cachedFile == null) {
				cachedFile = new CachedFile();
				cachedFile.setCacheName(mCacheName);
				try {
					cachedFile.setFileName(file.getCanonicalPath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cachedFile.setFileSize(file.length());
				cachedFile.setLibrary(mLibrary);
				cachedFile.setUniqueKey(uniqueKey);
			}
			
			cachedFile.setLastAccessedTime(Calendar.getInstance());
			
			try {
				handler.getAccessObject(CachedFile.class).createOrUpdate(cachedFile);
			} catch (SQLException e) {
				mLogger.error("Error updating cached file", e);
			}
		} finally {
			handler.close();
			FlushCacheTask.doFlush(mContext, mCacheName, mMaxSize);
		}
	}
	
	public File get(String uniqueKey) {
		final DatabaseHandler handler = new DatabaseHandler(mContext);
		try {
			final CachedFile cachedFile = getCachedFile(handler, mLibrary.getId(), mCacheName, uniqueKey);
			if (cachedFile != null) {
				cachedFile.setLastAccessedTime(Calendar.getInstance());
				try {
					handler.getAccessObject(CachedFile.class).update(cachedFile);
				} catch (SQLException e) {
					mLogger.error("Error updating cached file entity", e);
				}
			}
			return cachedFile != null ? new File(cachedFile.getFileName()) : null;
		} finally {
			handler.close();
		}
	}
	
	public boolean containsKey(String uniqueKey) {
		return get(uniqueKey) != null;
	}
	
	private final static CachedFile getCachedFile(final DatabaseHandler handler, final int libraryId, final String cacheName, final String uniqueKey) {
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.where()
						.eq("libraryId", libraryId)
						.and()
						.eq("cacheName", cacheName)
						.and()
						.eq("uniqueKey", uniqueKey).prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);			
		} catch (SQLException e) {
			mLogger.error("SQLException", e);
			return null;
		}
	}
	

	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	public final static java.io.File getDiskCacheDir(final Context context, final String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath =
	            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ?
            		context.getExternalCacheDir().getPath() :
                    context.getCacheDir().getPath();

	    return new java.io.File(cachePath + java.io.File.separator + uniqueName);
	}
}
