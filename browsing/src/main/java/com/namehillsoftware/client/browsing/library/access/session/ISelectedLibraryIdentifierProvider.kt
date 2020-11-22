package com.namehillsoftware.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

/**
 * Created by david on 2/12/17.
 */
interface ISelectedLibraryIdentifierProvider {
	val selectedLibraryId: LibraryId?
}
