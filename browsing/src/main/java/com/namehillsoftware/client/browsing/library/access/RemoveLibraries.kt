package com.namehillsoftware.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

interface RemoveLibraries {
	fun removeLibrary(library: Library): Promise<*>
}
