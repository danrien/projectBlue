package com.lasthopesoftware.bluewater.client.library.items.media.files.properties

import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import java.util.*

class FilePropertiesProvider(private val libraryConnections: ProvideLibraryConnections, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> {
		return libraryConnections.promiseLibraryConnection(libraryId).eventually { connectionProvider ->
			RevisionChecker.promiseRevision(connectionProvider).eventually { revision: Int ->
				val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
				val filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder)

				if (filePropertiesContainer != null && filePropertiesContainer.properties.isNotEmpty() && revision == filePropertiesContainer.revision)
					Promise<Map<String, String>>(HashMap(filePropertiesContainer.properties))
				else
					FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile, revision)
			}
		}
	}
}
