package com.lasthopesoftware.bluewater.servers.library.repository;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.j256.ormlite.dao.Dao;
import com.lasthopesoftware.bluewater.disk.sqlite.access.RepositoryAccessHelper;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LibrarySession {
	
	private static final Logger mLogger = LoggerFactory.getLogger(LibrarySession.class);
	public static final String libraryChosenEvent = LibrarySession.class.getCanonicalName() + ".libraryChosenEvent";
	public static final String chosenLibraryInt = "chosen_library";

	public static void SaveLibrary(final Context context, final Library library) {
		SaveLibrary(context, library, null);
	}

	public static void SaveLibrary(final Context context, final Library library, final OnCompleteListener<Void, Void, Library> onSaveComplete) {

		final SimpleTask<Void, Void, Library> writeToDatabaseTask = new SimpleTask<>(new OnExecuteListener<Void, Void, Library>() {

			@Override
			public Library onExecute(ISimpleTask<Void, Void, Library> owner, Void... params) throws Exception {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					repositoryAccessHelper.getDataAccess(Library.class).createOrUpdate(library);
					mLogger.debug("Library saved.");
					return library;
				} catch (SQLException e) {
					mLogger.error(e.toString(), e);
				} finally {
					repositoryAccessHelper.close();
				}

				return null;
			}
		});

		if (onSaveComplete != null)
			writeToDatabaseTask.addOnCompleteListener(onSaveComplete);

		writeToDatabaseTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static void GetActiveLibrary(final Context context, final OnCompleteListener<Integer, Void, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new SimpleTask<>(new OnExecuteListener<Integer, Void, Library>() {

			@Override
			public Library onExecute(ISimpleTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				return GetActiveLibrary(context);
			}
		}), onGetLibraryComplete);
	}

	public static void GetLibrary(final Context context, final int libraryId, final OnCompleteListener<Integer, Void, Library> onGetLibraryComplete) {
		ExecuteGetLibrary(new SimpleTask<>(new OnExecuteListener<Integer, Void, Library>() {

			@Override
			public Library onExecute(ISimpleTask<Integer, Void, Library> owner, Integer... params) throws Exception {
				return GetLibrary(context, libraryId);
			}
		}), onGetLibraryComplete);
	}

	private static void ExecuteGetLibrary(SimpleTask<Integer, Void, Library> getLibraryTask, final OnCompleteListener<Integer, Void, Library> onGetLibraryComplete) {

		getLibraryTask.addOnCompleteListener(new OnCompleteListener<Integer, Void, Library>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
				if (onGetLibraryComplete != null)
					onGetLibraryComplete.onComplete(owner, result);
			}
		});

		getLibraryTask.execute(RepositoryAccessHelper.databaseExecutor);
	}

	public static synchronized Library GetActiveLibrary(final Context context) {
		if ("Main".equals(Thread.currentThread().getName()))
			throw new IllegalStateException("This method must be called from a background thread.");

		final int chosenLibraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(chosenLibraryInt, -1);
		return chosenLibraryId >= 0 ? GetLibrary(context, chosenLibraryId) : null;
	}

	private static synchronized Library GetLibrary(final Context context, int libraryId) {
		if (libraryId < 0) return null;

		final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
		try {
			final Dao<Library, Integer> libraryAccess = repositoryAccessHelper.getDataAccess(Library.class);
			return libraryAccess.queryForId(libraryId);
		} catch (SQLException e) {
			mLogger.error(e.toString(), e);
		} finally {
			repositoryAccessHelper.close();
		}

		return null;
	}
	
	public static void GetLibraries(final Context context, OnCompleteListener<Void, Void, List<Library>> onGetLibrariesComplete) {
		final SimpleTask<Void, Void, List<Library>> getLibrariesTask = new SimpleTask<>(new OnExecuteListener<Void, Void, List<Library>>() {
			
			@Override
			public List<Library> onExecute(ISimpleTask<Void, Void, List<Library>> owner, Void... params) throws Exception {
				final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context);
				try {
					return repositoryAccessHelper.getDataAccess(Library.class).queryForAll();
				} catch (SQLException e) {
					mLogger.error(e.toString(), e);
				} finally {
					repositoryAccessHelper.close();
				}
				
				return new ArrayList<>();
			}
		});

		if (onGetLibrariesComplete != null)
			getLibrariesTask.addOnCompleteListener(onGetLibrariesComplete);

		getLibrariesTask.execute(RepositoryAccessHelper.databaseExecutor);
	}
		
	public synchronized static void ChooseLibrary(final Context context, final int libraryKey, final OnCompleteListener<Integer, Void, Library> onLibraryChangeComplete) {

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryKey != sharedPreferences.getInt(chosenLibraryInt, -1)) {
            sharedPreferences.edit().putInt(chosenLibraryInt, libraryKey).apply();
		}
		
		GetActiveLibrary(context, new OnCompleteListener<Integer, Void, Library>() {
			@Override
			public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library library) {
				final Intent broadcastIntent = new Intent(libraryChosenEvent);
				broadcastIntent.putExtra(chosenLibraryInt, libraryKey);
				LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

				if (onLibraryChangeComplete != null)
					onLibraryChangeComplete.onComplete(owner, library);
			}
		});
	}
}
