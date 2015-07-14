package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreFilesService extends Service {

	private static class QueuedFileHolder {
		public final IFile file;
		public final String remotePath;

		private QueuedFileHolder(IFile file, String remotePath) {
			this.file = file;
			this.remotePath = remotePath;
		}
	}

	private static final String queueFileForDownload = StoreFilesService.class.getCanonicalName() + ".queueFileForDownload";

	private static final String destinationPathKey = StoreFilesService.class.getCanonicalName() + ".destinationPathKey";
	private static final String fileIdKey = StoreFilesService.class.getCanonicalName() + ".fileIdKey";

	private final Queue<QueuedFileHolder> mFileDownloadQueue = new ArrayDeque<>();
	private final Set<Integer> mQueuedFileKeys = new HashSet<>();

	private static final ExecutorService mStoreFilesExecutor = Executors.newSingleThreadExecutor();

	private static final Logger mLogger = LoggerFactory.getLogger(StoreFilesService.class);

	private boolean mIsForeground;

	public static void queueFileForDownload(Context context, IFile file, StoredFile storedFile) {
		final Intent intent = new Intent(context, StoreFilesService.class);
		intent.setAction(queueFileForDownload);
		intent.putExtra(destinationPathKey, storedFile.getPath());
		intent.putExtra(fileIdKey, file.getKey());
		context.startService(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final int returnValue = START_NOT_STICKY;

		if (!intent.getAction().equals(queueFileForDownload)) return returnValue;

		final int fileKey = intent.getIntExtra(fileIdKey, -1);
		if (fileKey == -1) return returnValue;

		final String remotePath = intent.getStringExtra(destinationPathKey);
		if (remotePath == null) return returnValue;

		queueAndStartDownloading(fileKey, remotePath);

		return returnValue;
	}

	private Notification buildSyncNotification() {
		final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this);
		notifyBuilder.setSmallIcon(R.drawable.clearstream_logo_dark);
		notifyBuilder.setContentTitle("Syncing files");
		return notifyBuilder.build();
	}

	private void queueAndStartDownloading(final int fileKey, final String remotePath) {
		if (!mQueuedFileKeys.add(fileKey)) return;

		mStoreFilesExecutor.execute(new Runnable() {
			@Override
			public void run() {
				if (!mIsForeground)
					startForeground(23, buildSyncNotification());

				final File file = new File(fileKey);

				HttpURLConnection connection;
				try {
					connection = ConnectionProvider.getConnection(file.getPlaybackParams());
				} catch (IOException e) {
					mLogger.error("Error opening connection", e);
					return;
				}

				if (connection == null) return;

				try {
					InputStream is;
					try {
						is = connection.getInputStream();
					} catch (IOException ioe) {
						mLogger.error("Error reading data from connection", ioe);
						return;
					}

					try {
						final FileOutputStream fos = new FileOutputStream(remotePath);
						try {
							IOUtils.copy(is, fos);
							fos.flush();
						} finally {
							fos.close();
						}
					} catch (IOException ioe) {
						mLogger.error("Error writing file!", ioe);
					}
					finally {
						if (is != null) {
							try {
								is.close();
							} catch (IOException e) {
								mLogger.error("Error closing input stream", e);
							}
						}
					}
				} finally {
					connection.disconnect();
					mQueuedFileKeys.remove(fileKey);

					if (mIsForeground && mQueuedFileKeys.size() == 0) {
						stopForeground(true);
						mIsForeground = false;
					}
				}
			}
		});
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class StoreMusicServiceBinder extends Binder {
		StoreFilesService getService() {
            return StoreFilesService.this;
        }
    }
	
	private final IBinder mBinder = new StoreMusicServiceBinder();
}
