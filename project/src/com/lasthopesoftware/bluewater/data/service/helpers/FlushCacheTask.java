package com.lasthopesoftware.bluewater.data.service.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.SelectArg;
import com.lasthopesoftware.bluewater.data.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.data.sqlite.objects.CachedFile;

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 *
 */
public class FlushCacheTask extends AsyncTask<Void, Void, Void> {

	private final static Logger mLogger = LoggerFactory.getLogger(FlushCacheTask.class);
	
	private final Context mContext;
	private final String mCacheName;
	private final long mTargetSize;
	private final long mExpirationTime;
	
	/*
	 * Flush a given cache until it reaches the given target size
	 */
	public static void doFlush(final Context context, final String cacheName, final long expirationTime, final long targetSize) {
		final FlushCacheTask task = new FlushCacheTask(context, cacheName, expirationTime, targetSize);
		task.executeOnExecutor(DatabaseHandler.databaseExecutor);
	}
	
	private FlushCacheTask(final Context context, final String cacheName, final long expirationTime, final long targetSize) {
		mContext = context;
		mCacheName = cacheName;
		mTargetSize = targetSize;
		mExpirationTime = expirationTime;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		final DatabaseHandler handler = new DatabaseHandler(mContext);
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
			
			// remove expired files
			final List<CachedFile> expiredFiles = getCachedFilePastTime(cachedFileAccess, mCacheName, System.currentTimeMillis() - mExpirationTime);
			for (CachedFile cachedFile : expiredFiles)
				deleteCachedFile(cachedFileAccess, cachedFile);
			
			if (getCachedFileSizeFromDatabase(cachedFileAccess, mCacheName) <= mTargetSize) return null;
			
			while (getCachedFileSizeFromDatabase(cachedFileAccess, mCacheName) > mTargetSize) {
				final CachedFile cachedFile = getOldestCachedFile(cachedFileAccess, mCacheName);
				if (cachedFile != null)
					deleteCachedFile(cachedFileAccess, cachedFile);
			}
			
			// Remove any files in the cache dir but not in the database			
			final File[] filesInCacheDir = FileCache.getDiskCacheDir(mContext, mCacheName).listFiles();
			
			// If the # of files in the cache dir is equal to the database size, then
			// hypothetically (and good enough for our purposes), they are in sync and we don't need
			// to do additional processing
			if (filesInCacheDir == null || filesInCacheDir.length == getCachedFileCount(cachedFileAccess, mCacheName))
				return null;
			
			for (int i = 0; i < filesInCacheDir.length; i++) {
				try {
					if (getCachedFileByFilename(cachedFileAccess, filesInCacheDir[i].getCanonicalPath()) != null) continue;
				} catch (IOException e) {
					mLogger.warn("Issue getting canonical file path.");
				}
				filesInCacheDir[i].delete();
			}
		} catch (SQLException accessException) {
			mLogger.error("Error accessing cache", accessException);
		} finally {
			handler.close();
		}
		
		return null;
	}

	private final static long getCachedFileSizeFromDatabase(final Dao<CachedFile, Integer> cachedFileAccess, final String cacheName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg())
						.prepare();
			
			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), cacheName);
		} catch (SQLException e) {
			mLogger.error("Error getting file size", e);
			return -1;
		}
	}
	
	private final static CachedFile getOldestCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final String cacheName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.orderBy(CachedFile.LAST_ACCESSED_TIME, true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);			
		} catch (SQLException e) {
			mLogger.error("Error getting oldest cached file", e);
			return null;
		}
	}
	
	private final static List<CachedFile> getCachedFilePastTime(final Dao<CachedFile, Integer> cachedFileAccess, final String cacheName, final long time) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.orderBy(CachedFile.LAST_ACCESSED_TIME, true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.and()
						.lt(CachedFile.CREATED_TIME, new SelectArg(time))
						.prepare();
			
			return cachedFileAccess.query(preparedQuery);			
		} catch (SQLException e) {
			mLogger.error("Error getting oldest cached file", e);
			return new ArrayList<CachedFile>();
		}
	}
	
	private final static long getCachedFileCount(final Dao<CachedFile, Integer> cachedFileAccess, final String cacheName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.setCountOf(true)
						.where()
						.eq(CachedFile.CACHE_NAME, new SelectArg(cacheName))
						.prepare();
			
			return cachedFileAccess.countOf(preparedQuery);
		} catch (SQLException e) {
			mLogger.error("Error getting file count", e);
			return -1;
		}
	}
	
	private final static CachedFile getCachedFileByFilename(final Dao<CachedFile, Integer> cachedFileAccess, final String fileName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.where()
						.eq(CachedFile.FILE_NAME, new SelectArg(fileName))
						.prepare();
			
			return cachedFileAccess.queryForFirst(preparedQuery);	
		} catch (SQLException e) {
			mLogger.error("Error getting cached file by filename", e);
			return null;
		}
	}
	
	private final static boolean deleteCachedFile(final Dao<CachedFile, Integer> cachedFileAccess, final CachedFile cachedFile) {
		final File fileToDelete = new File(cachedFile.getFileName());
		if (fileToDelete.exists()) 
			fileToDelete.delete();
		
		try {
			cachedFileAccess.delete(cachedFile);
			return true;
		} catch (SQLException deleteException) {
			mLogger.error("Error deleting file pointer from database", deleteException);
		}
		
		return false;
	}
}
