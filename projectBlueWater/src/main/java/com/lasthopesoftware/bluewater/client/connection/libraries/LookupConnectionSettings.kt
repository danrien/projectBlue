package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface LookupConnectionSettings {
	fun lookupConnectionSettings(libraryId: LibraryId): Promise<ConnectionSettings?>
}
