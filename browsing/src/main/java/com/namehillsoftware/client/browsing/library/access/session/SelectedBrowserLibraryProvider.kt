package com.namehillsoftware.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.client.browsing.library.access.ILibraryProvider
import com.namehillsoftware.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/12/17.
 */
class SelectedBrowserLibraryProvider(private val selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider, private val libraryProvider: ILibraryProvider) : ISelectedBrowserLibraryProvider {
	override fun getBrowserLibrary(): Promise<Library?> {
		val selectedLibraryId = selectedLibraryIdentifierProvider.selectedLibraryId
			?: return Promise.empty()
		return libraryProvider.getLibrary(selectedLibraryId)
	}
}
