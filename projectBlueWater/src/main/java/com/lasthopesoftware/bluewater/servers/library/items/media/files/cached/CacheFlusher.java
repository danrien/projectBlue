package com.lasthopesoftware.bluewater.servers.library.items.media.files.cached;

import android.content.Context;

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.cached.repository.CachedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Flush a given cache until it reaches the given target size
 * @author david
 *
 */
class CacheFlusher implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(CacheFlusher.class);
	
	private final Context context;
	private final String cacheName;
	private final long targetSize;
	
	/*
	 * Flush a given cache until it reaches the given target size
	 */
	public static void doFlush(final Context context, final String cacheName, final long targetSize) {
		RepositoryAccessHelper.databaseExecutor.execute(new CacheFlusher(context, cacheName, targetSize));
	}

	public static void doFlushSynchronously(final Context context, final String cacheName, final long targetSize) {
		(new CacheFlusher(context, cacheName, targetSize)).run();
	}
	
	private CacheFlusher(final Context context, final String cacheName, final long targetSize) {
		this.context = context;
		this.cacheName = cacheName;
		this.targetSize = targetSize;
	}
	
	@Override
	public final void run() {
		final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
		try {
			if (getCachedFileSizeFromDatabase(repositoryAccessHelper) <= targetSize) return;
			
			while (getCachedFileSizeFromDatabase(repositoryAccessHelper) > targetSize) {
				final CachedFile cachedFile = getOldestCachedFile(repositoryAccessHelper);
				if (cachedFile != null)
					deleteCachedFile(repositoryAccessHelper, cachedFile);
			}
			
			// Remove any files in the cache dir but not in the database
			final File cacheDir = DiskFileCache.getDiskCacheDir(context, cacheName);
			
			if (cacheDir == null || !cacheDir.exists()) return;
			
			final File[] filesInCacheDir = cacheDir.listFiles();
			
			// If the # of files in the cache dir is equal to the database size, then
			// hypothetically (and good enough for our purposes), they are in sync and we don't need
			// to do additional processing
			if (filesInCacheDir == null || filesInCacheDir.length == getCachedFileCount(repositoryAccessHelper))
				return;
			
			for (File fileInCacheDir : filesInCacheDir) {
				try {
					if (getCachedFileByFilename(repositoryAccessHelper, fileInCacheDir.getCanonicalPath()) != null) continue;
				} catch (IOException e) {
					logger.warn("Issue getting canonical file path.");
				}
				fileInCacheDir.delete();
			}
		} finally {
			repositoryAccessHelper.close();
		}
	}

	private long getCachedFileSizeFromDatabase(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT SUM(" + CachedFile.FILE_SIZE + ") FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME)
				.addParameter(CachedFile.CACHE_NAME, cacheName)
				.execute();
	}
	
//	private final long getCacheSizeBetweenTimes(final Dao<CachedFile, Integer> cachedFileAccess, final long startTime, final long endTime) {
//		try {
//			
//			final PreparedQuery<CachedFile> preparedQuery =
//					cachedFileAccess.queryBuilder()
//						.selectRaw("SUM(" + CachedFile.FILE_SIZE + ")")
//						.where()
//						.eq(CachedFile.CACHE_NAME, new SelectArg())
//						.and()
//						.between(CachedFile.CREATED_TIME, new SelectArg(), new SelectArg())
//						.prepare();
//			
//			return cachedFileAccess.queryRawValue(preparedQuery.getStatement(), cacheName, String.valueOf(startTime), String.valueOf(endTime));
//		} catch (SQLException e) {
//			logger.error("Error getting file size", e);
//			return -1;
//		}
//	}
	
	private CachedFile getOldestCachedFile(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT * FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME + " ORDER BY " + CachedFile.LAST_ACCESSED_TIME + " ASC")
				.addParameter(CachedFile.CACHE_NAME, cacheName)
				.fetchFirst(CachedFile.class);
	}
	
	private long getCachedFileCount(final RepositoryAccessHelper repositoryAccessHelper) {
		return repositoryAccessHelper
				.mapSql("SELECT COUNT(*) FROM " + CachedFile.tableName + " WHERE " + CachedFile.CACHE_NAME + " = @" + CachedFile.CACHE_NAME)
				.addParameter(CachedFile.CACHE_NAME, cacheName)
				.execute();
	}
	
	private static CachedFile getCachedFileByFilename(final RepositoryAccessHelper repositoryAccessHelper, final String fileName) {

		return repositoryAccessHelper
				.mapSql("SELECT * FROM " + CachedFile.tableName + " WHERE " + CachedFile.FILE_NAME + " = @" + CachedFile.FILE_NAME)
				.addParameter(CachedFile.FILE_NAME, fileName)
				.fetchFirst(CachedFile.class);

	}
	
	private static boolean deleteCachedFile(final RepositoryAccessHelper repositoryAccessHelper, final CachedFile cachedFile) {
		final File fileToDelete = new File(cachedFile.getFileName());

		if (fileToDelete.exists() && fileToDelete.delete())
			repositoryAccessHelper
					.mapSql("DELETE FROM " + CachedFile.tableName + " WHERE id = @id")
					.addParameter("id", cachedFile.getId())
					.execute();

		return true;
	}
}
