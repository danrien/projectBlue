package com.namehillsoftware.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/11/17.
 */
interface ILibraryStorage {
	fun saveLibrary(library: Library): Promise<Library>

	fun removeLibrary(library: Library): Promise<Any?>
}
