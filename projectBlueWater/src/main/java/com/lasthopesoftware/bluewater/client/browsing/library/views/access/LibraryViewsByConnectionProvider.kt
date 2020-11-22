package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemResponse
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response

class LibraryViewsByConnectionProvider : ProvideLibraryViewsUsingConnection {
	override fun promiseLibraryViewsFromConnection(connectionProvider: IConnectionProvider): Promise<Collection<ViewItem>> {
		return com.namehillsoftware.client.browsing.library.access.RevisionChecker.promiseRevision(connectionProvider)
			.eventually { serverRevision: Int ->
				synchronized(browseLibraryParameter) {
					if (cachedFileSystemItems != null && revision == serverRevision)
						return@eventually Promise(cachedFileSystemItems!!)
				}

				connectionProvider.promiseResponse(browseLibraryParameter)
					.then { response: Response ->
						response.body?.use { b ->
							b.byteStream().use { s ->
								val viewItems = ItemResponse.GetItems(s)
									.map { i ->
										when (i.value) {
											KnownViews.Playlists -> PlaylistViewItem(i.key)
											else -> StandardViewItem(i.key, i.value)
										}
									}

								synchronized(browseLibraryParameter) {
									revision = serverRevision
									cachedFileSystemItems = viewItems
								}

								return@then viewItems
							}
						} ?: return@then emptySet<ViewItem>()
					}
			}
	}

	companion object {
		const val browseLibraryParameter = "Browse/Children"
		private var cachedFileSystemItems: Collection<ViewItem>? = null
		private var revision: Int? = null
	}
}
