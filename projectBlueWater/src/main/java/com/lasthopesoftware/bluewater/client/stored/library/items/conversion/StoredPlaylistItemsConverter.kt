package com.lasthopesoftware.bluewater.client.stored.library.items.conversion

import com.lasthopesoftware.bluewater.client.browsing.items.playlists.FindPlaylistItem
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemHelpers
import com.namehillsoftware.handoff.promises.Promise

class StoredPlaylistItemsConverter(private val libraryProvider: com.namehillsoftware.client.browsing.library.access.session.ISelectedBrowserLibraryProvider, private val playlistItem: FindPlaylistItem, private val storedItemAccess: IStoredItemAccess) : ConvertStoredPlaylistsToStoredItems {
	override fun promiseConvertedStoredItem(storedItem: StoredItem): Promise<StoredItem> =
		if (storedItem.itemType != StoredItem.ItemType.PLAYLIST) Promise(storedItem)
		else libraryProvider.browserLibrary
			.eventually { l ->
				val playlist = Playlist(storedItem.serviceId)
				storedItemAccess.toggleSync(l.libraryId, playlist, false)
				playlistItem.promiseItem(playlist)
					.eventually { item ->
						if (item != null)
							storedItemAccess.toggleSync(l.libraryId, item, true)
						storedItemAccess.promiseStoredItems(l.libraryId)
							.then { storedItems ->
								storedItems
									.single { i -> i.serviceId == item?.key && i.itemType == StoredItemHelpers.getListType(item) }
							}
					}
			}
}
