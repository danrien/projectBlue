package com.namehillsoftware.client.browsing.library.access.session;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/19/17.
 */
public class BrowserLibrarySelection implements IBrowserLibrarySelection {

	public static final String libraryChosenEvent = MagicPropertyBuilder.buildMagicPropertyName(BrowserLibrarySelection.class, "libraryChosenEvent");

	private final Context context;
	private final LocalBroadcastManager localBroadcastManager;
	private final ILibraryProvider libraryProvider;

	public BrowserLibrarySelection(Context context, LocalBroadcastManager localBroadcastManager, ILibraryProvider libraryProvider) {
		this.context = context;
		this.localBroadcastManager = localBroadcastManager;
		this.libraryProvider = libraryProvider;
	}

	@Override
	public Promise<Library> selectBrowserLibrary(LibraryId libraryId) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (libraryId.getId() == sharedPreferences.getInt(LibrarySelectionKey.chosenLibraryKey, -1))
			return libraryProvider.getLibrary(libraryId);

		sharedPreferences.edit().putInt(LibrarySelectionKey.chosenLibraryKey, libraryId.getId()).apply();

		final Intent broadcastIntent = new Intent(libraryChosenEvent);
		broadcastIntent.putExtra(LibrarySelectionKey.chosenLibraryKey, libraryId.getId());
		localBroadcastManager.sendBroadcast(broadcastIntent);

		return libraryProvider.getLibrary(libraryId);
	}
}
