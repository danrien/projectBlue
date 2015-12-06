package com.lasthopesoftware.bluewater.servers.library.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.FileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredFileDownloader;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.StoredItemAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.runnables.IOneParameterRunnable;
import com.lasthopesoftware.threading.IFluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 8/30/15.
 */
public class LibrarySyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(LibrarySyncHandler.class);

	private final Context context;
	private final ConnectionProvider connectionProvider;
	private final Library library;
	private final StoredFileDownloader storedFileDownloader;

	private volatile boolean isCancelled;

	public LibrarySyncHandler(Context context, ConnectionProvider connectionProvider, Library library) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.library = library;
		this.storedFileDownloader = new StoredFileDownloader(context, connectionProvider, library);
	}

	public void setOnFileDownloaded(IOneParameterRunnable<StoredFile> onFileDownloaded) {
		storedFileDownloader.setOnFileDownloaded(onFileDownloaded);
	}

	public void setOnFileQueued(IOneParameterRunnable<StoredFile> onFileQueued) {
		storedFileDownloader.setOnFileQueued(onFileQueued);
	}

	public void setOnQueueProcessingCompleted(final IOneParameterRunnable<LibrarySyncHandler> onQueueProcessingCompleted) {
		storedFileDownloader.setOnQueueProcessingCompleted(new Runnable() {
			@Override
			public void run() {
				onQueueProcessingCompleted.run(LibrarySyncHandler.this);
			}
		});
	}

	public void cancel() {
		isCancelled = true;

		storedFileDownloader.cancel();
	}

	public void startSync() {
		final StoredItemAccess storedItemAccess = new StoredItemAccess(context, library);
		storedItemAccess.getStoredItems(new IFluentTask.OnCompleteListener<Void, Void, List<StoredItem>>() {

			@Override
			public void onComplete(IFluentTask<Void, Void, List<StoredItem>> owner, final List<StoredItem> storedItems) {
				AsyncTask
					.THREAD_POOL_EXECUTOR
					.execute(new Runnable() {
						@Override
						public void run() {
							if (isCancelled) return;

							final Set<Integer> allSyncedFileKeys = new HashSet<>();
							final StoredFileAccess storedFileAccess = new StoredFileAccess(context, library);

							for (StoredItem storedItem : storedItems) {
								if (isCancelled) return;

								final int serviceId = storedItem.getServiceId();
								final FileProvider fileProvider = new FileProvider(connectionProvider, storedItem.getItemType() == StoredItem.ItemType.ITEM ? new Item(serviceId) : new Playlist(serviceId));

								try {
									final List<IFile> files = fileProvider.get();
									for (final IFile file : files) {
										allSyncedFileKeys.add(file.getKey());

										if (isCancelled) return;

										final StoredFile storedFile = storedFileAccess.createOrUpdateFile(file);
										if (!storedFile.isDownloadComplete())
											storedFileDownloader.queueFileForDownload(file, storedFile);
									}
								} catch (ExecutionException | InterruptedException e) {
									logger.warn("There was an error retrieving the files", e);
								}
							}

							if (isCancelled) return;

							storedFileDownloader.process();

							if (isCancelled) return;

							storedFileAccess.pruneStoredFiles(allSyncedFileKeys);
						}
					});
			}
		});
	}
}
