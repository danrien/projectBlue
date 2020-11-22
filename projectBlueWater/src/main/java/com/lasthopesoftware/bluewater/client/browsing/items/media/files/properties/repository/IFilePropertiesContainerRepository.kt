package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

/**
 * Created by david on 3/14/17.
 */
interface IFilePropertiesContainerRepository {
	fun getFilePropertiesContainer(key: com.namehillsoftware.projectblue.shared.UrlKeyHolder<ServiceFile>): FilePropertiesContainer?
	fun putFilePropertiesContainer(key: com.namehillsoftware.projectblue.shared.UrlKeyHolder<ServiceFile>, filePropertiesContainer: FilePropertiesContainer)
}
