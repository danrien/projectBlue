package com.namehillsoftware.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/18/17.
 */
interface ILibraryProvider {
	fun getLibrary(libraryId: LibraryId): Promise<Library?>
	val allLibraries: Promise<Collection<Library>>
}
