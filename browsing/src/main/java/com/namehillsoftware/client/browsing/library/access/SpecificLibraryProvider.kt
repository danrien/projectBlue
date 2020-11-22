package com.namehillsoftware.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/12/17.
 */
class SpecificLibraryProvider(private val libraryId: LibraryId, private val libraryProvider: ILibraryProvider) : ISpecificLibraryProvider {
	override val library: Promise<Library?>
		get() = libraryProvider.getLibrary(libraryId)
}
