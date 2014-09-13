package com.lasthopesoftware.bluewater.data.service.helpers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
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
	private static final ExecutorService flushExecutor = Executors.newSingleThreadExecutor();
	
	/*
	 * Flush a given cache until it reaches the given target size
	 */
	public static void doFlush(final Context context, final String cacheName, final long targetSize) {
		final FlushCacheTask task = new FlushCacheTask(context, cacheName, targetSize);
		task.executeOnExecutor(flushExecutor);
	}
	
	private FlushCacheTask(final Context context, final String cacheName, final long targetSize) {
		mContext = context;
		mCacheName = cacheName;
		mTargetSize = targetSize;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		final DatabaseHandler handler = new DatabaseHandler(mContext);
		try {
			final Dao<CachedFile, Integer> cachedFileAccess = handler.getAccessObject(CachedFile.class);
		
			List<CachedFile> allCachedFiles = getAllCachedFiles(cachedFileAccess,  mCacheName);
			
			while (calculateTotalSize(allCachedFiles) > mTargetSize) {
				final CachedFile cachedFile = allCachedFiles.get(0);
				final File fileToDelete = new File(cachedFile.getFileName());
				if (fileToDelete.exists()) 
					fileToDelete.delete();
				
				try {
					handler.getAccessObject(CachedFile.class).delete(cachedFile);
				} catch (SQLException deleteException) {
					mLogger.error("Error deleting file pointer from database", deleteException);
					// Reset the cached files list
					allCachedFiles = getAllCachedFiles(cachedFileAccess,  mCacheName);
					continue;
				}
				
				allCachedFiles.remove(cachedFile);
			}
			
			// Remove any files in the cache dir but not in the database
			final File cachedFileDir = FileCache.getDiskCacheDir(mContext, mCacheName);
			final File[] filesInCacheDir = cachedFileDir.listFiles();
			if (filesInCacheDir == null)
				return null;
			
			for (int i = 0; i < filesInCacheDir.length; i++) {
				boolean isFileFound = false;
				for (CachedFile cachedFile : allCachedFiles) {
					try {
						if (cachedFile.getFileName() == filesInCacheDir[i].getCanonicalPath()) {
							isFileFound = true;
							break;
						}
					} catch (IOException e) {
						mLogger.warn("Issue getting canonical file path.");
					}
				}
				
				// File wasn't found in cache, it shouldn't be here so delete it
				if (!isFileFound)
					filesInCacheDir[i].delete();
			}
		} catch (SQLException accessException) {
			mLogger.error("Error accessing cache", accessException);
		} finally {
			handler.close();
		}
		
		return null;
	}

	private final static int calculateTotalSize(final List<CachedFile> cachedFiles) {
		int returnSize = 0;
		for (CachedFile cachedFile : cachedFiles)
			returnSize += cachedFile.getFileSize();
		
		return returnSize;
	}
	
	private final static List<CachedFile> getAllCachedFiles(final Dao<CachedFile, Integer> cachedFileAccess, final String cacheName) {
		try {
			
			final PreparedQuery<CachedFile> preparedQuery =
					cachedFileAccess.queryBuilder()
						.orderBy("lastAccessedTime", true)
						.where()
						.eq("cacheName", cacheName)
						.prepare();
			
			return cachedFileAccess.query(preparedQuery);			
		} catch (SQLException e) {
			mLogger.error("Error getting file list", e);
			return new ArrayList<CachedFile>();
		}
	}

}
