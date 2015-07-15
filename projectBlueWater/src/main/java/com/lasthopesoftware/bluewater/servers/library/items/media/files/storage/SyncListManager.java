package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.lasthopesoftware.bluewater.disk.sqlite.access.DatabaseHandler;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredList;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by david on 7/5/15.
 */
public class SyncListManager {

    private static final Logger mLogger = LoggerFactory.getLogger(SyncListManager.class);

    private final Context mContext;

    public SyncListManager(Context context) {
        mContext = context;
    }

    public void startSync() {
        DatabaseHandler.databaseExecutor.execute(new Runnable() {
	        @Override
	        public void run() {
		        final Library library = LibrarySession.GetLibrary(mContext);
		        if (library == null) return;

		        final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
		        try {
			        final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
			        final List<StoredList> listsToSync = storedListAccess.queryForAll();

			        final Dao<StoredFile, Integer> storedFileAccessor = dbHandler.getAccessObject(StoredFile.class);
			        final StoredFileAccess storedFileAccess = new StoredFileAccess(mContext, library);

			        final Set<Integer> allSyncedFileKeys = new HashSet<>();
			        for (StoredList listToSync : listsToSync) {
				        final int serviceId = listToSync.getServiceId();
				        final IFilesContainer filesContainer = listToSync.getType() == StoredList.ListType.ITEM ? new Item(serviceId) : new Playlist(serviceId);
				        final ArrayList<IFile> files = filesContainer.getFiles().getFiles();
				        for (IFile file : files)
					        allSyncedFileKeys.add(file.getKey());

				        storedFileAccess.syncFilesSynchronously(storedFileAccessor, files);
			        }

			        storedFileAccess.pruneStoredFiles(allSyncedFileKeys);
		        } catch (SQLException e) {
			        mLogger.error("Error accessing the stored list access", e);
		        } finally {
			        dbHandler.close();
		        }
	        }
        });
    }

    public void markItemForSync(IItem item) {
        final StoredList.ListType listType = item instanceof Playlist ? StoredList.ListType.PLAYLIST : StoredList.ListType.ITEM;
        markItemForSync(item, listType);
    }

    public void isItemMarkedForSync(final IItem item, ISimpleTask.OnCompleteListener<Void, Void, Boolean> isItemSyncedResult) {
        final SimpleTask<Void, Void, Boolean> isItemSyncedTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Void, Void, Boolean>() {
            @Override
            public Boolean onExecute(ISimpleTask<Void, Void, Boolean> owner, Void... params) throws Exception {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);
                    return isItemMarkedForSync(storedListAccess, LibrarySession.GetLibrary(mContext), item);
                } finally {
                    dbHandler.close();
                }
            }
        });

        if (isItemSyncedResult != null)
            isItemSyncedTask.addOnCompleteListener(isItemSyncedResult);

        isItemSyncedTask.execute(DatabaseHandler.databaseExecutor);
    }

    private void markItemForSync(final IItem item, final StoredList.ListType listType) {
        DatabaseHandler.databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final DatabaseHandler dbHandler = new DatabaseHandler(mContext);
                try {
                    final Dao<StoredList, Integer> storedListAccess = dbHandler.getAccessObject(StoredList.class);

                    final Library library = LibrarySession.GetLibrary(mContext);
                    if (isItemMarkedForSync(storedListAccess, library, item)) return;

                    final StoredList storedList = new StoredList();
                    storedList.setLibrary(library);
                    storedList.setServiceId(item.getKey());
                    storedList.setType(listType);

                    try {
                        storedListAccess.create(storedList);
                    } catch (SQLException e) {
                        mLogger.error("Error while creating new stored list", e);
                    }
                } catch (SQLException e) {
                    mLogger.error("Error getting access to the stored list table", e);
                } finally {
                    dbHandler.close();
                }
            }
        });
    }

    private static boolean isItemMarkedForSync(Dao<StoredList, Integer> storedListAccess, Library library, IItem item) {
        try {
            final PreparedQuery<StoredList> storedListPreparedQuery =
                    storedListAccess
                            .queryBuilder()
                            .where()
                            .eq(StoredFile.serviceIdColumnName, item.getKey())
                            .and()
                            .eq(StoredFile.libraryIdColumnName, library.getId())
                            .prepare();
            return storedListAccess.queryForFirst(storedListPreparedQuery) != null;
        } catch (SQLException e) {
            mLogger.error("Error while checking whether stored list exists.", e);
        }

        return false;
    }
}
