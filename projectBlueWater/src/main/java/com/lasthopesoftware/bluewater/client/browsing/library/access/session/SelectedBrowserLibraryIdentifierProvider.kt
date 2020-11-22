package com.lasthopesoftware.bluewater.client.browsing.library.access.session

import android.content.Context
import androidx.preference.PreferenceManager
import com.namehillsoftware.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.namehillsoftware.client.browsing.library.access.session.LibrarySelectionKey
import com.namehillsoftware.client.browsing.library.repository.LibraryId

/**
 * Created by david on 2/12/17.
 */
class SelectedBrowserLibraryIdentifierProvider(private val context: Context) : ISelectedLibraryIdentifierProvider {
	override val selectedLibraryId: LibraryId?
		get() {
			val libraryId = PreferenceManager.getDefaultSharedPreferences(context).getInt(LibrarySelectionKey.chosenLibraryKey, -1)
			return if (libraryId > -1) LibraryId(libraryId) else null
		}
}
