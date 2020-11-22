package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import java.util.*

open class SessionFilePropertiesProvider(private val connectionProvider: IConnectionProvider, private val filePropertiesContainerProvider: IFilePropertiesContainerRepository) : ProvideFilePropertiesForSession {
	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> {
		return com.namehillsoftware.client.browsing.library.access.RevisionChecker.promiseRevision(connectionProvider).eventually { revision: Int ->
			val urlKeyHolder = com.namehillsoftware.projectblue.shared.UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
			val filePropertiesContainer = filePropertiesContainerProvider.getFilePropertiesContainer(urlKeyHolder)

			if (filePropertiesContainer != null && filePropertiesContainer.properties.isNotEmpty() && revision == filePropertiesContainer.revision) {
				Promise<Map<String, String>>(HashMap(filePropertiesContainer.properties))
			} else {
				FilePropertiesPromise(connectionProvider, filePropertiesContainerProvider, serviceFile, revision)
			}
		}
	}

}
