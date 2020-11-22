package com.namehillsoftware.client.browsing.library.access.session

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId

class StaticLibraryIdentifierProvider constructor(instanceLibraryProvider: ISelectedLibraryIdentifierProvider) : ISelectedLibraryIdentifierProvider {
	private val libraryId = instanceLibraryProvider.selectedLibraryId

	override val selectedLibraryId: LibraryId?
		get() = libraryId
}
