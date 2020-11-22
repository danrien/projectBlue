package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 3/7/16.
 */
class CachedFilePropertiesProvider(private val libraryConnections: ProvideLibraryConnections, private val filePropertiesContainerRepository: IFilePropertiesContainerRepository, private val filePropertiesProvider: ProvideLibraryFileProperties) : ProvideLibraryFileProperties {
	override fun promiseFileProperties(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Map<String, String>> {
		return libraryConnections.promiseLibraryConnection(libraryId)
			.eventually { connectionProvider ->
				val urlKeyHolder = com.namehillsoftware.projectblue.shared.UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
				when(val filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)) {
					null -> filePropertiesProvider.promiseFileProperties(libraryId, serviceFile)
					else -> Promise(filePropertiesContainer.properties)
				}
			}
	}
}
