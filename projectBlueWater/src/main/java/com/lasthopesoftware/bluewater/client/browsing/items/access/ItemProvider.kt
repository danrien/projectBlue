package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse.parseItems
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.LibraryViewsByConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory
import java.io.IOException

class ItemProvider(private val connectionProvider: IConnectionProvider) : ProvideItems {

	companion object {
		private val logger = LoggerFactory.getLogger(ItemProvider::class.java)

		fun IConnectionProvider.promiseItems(itemKey: Int): Promise<List<Item>> =
			ItemProvider(this).promiseItems(itemKey)
	}

	override fun promiseItems(itemKey: Int): Promise<List<Item>> =
		connectionProvider
			.promiseResponse(
				LibraryViewsByConnectionProvider.browseLibraryParameter,
				"ID=$itemKey",
				"Version=2")
			.then { response ->
				response.body?.let { body ->
					try {
						body.byteStream().use { parseItems(it) }
					} catch (e: IOException) {
						logger.error("There was an error getting the inputstream", e)
						throw e
					} finally {
						body.close()
					}
				} ?: emptyList()
			}
}
