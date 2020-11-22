package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise

class PlaylistItemFinder(private val libraryViews: ProvideLibraryViews, private val itemProvider: ProvideItems) : FindPlaylistItem {
	override fun promiseItem(playlist: Playlist): Promise<Item?> = libraryViews.promiseLibraryViews()
		.eventually { v ->
			val playlistItem = v.single { i -> KnownViews.Playlists == i.value }
			recursivelySearchForPlaylist(playlistItem, playlist)
		}

	private fun recursivelySearchForPlaylist(rootItem: Item, playlist: Playlist): Promise<Item?> =
		itemProvider.promiseItems(rootItem.key)
			.eventually { items ->
				val possiblePlaylistItem = items.firstOrNull { it?.playlistId == playlist.key }
				possiblePlaylistItem?.toPromise<Item?>()
					?: Promise.whenAll(items.map { recursivelySearchForPlaylist(it, playlist) })
						.then { aggregatedItems -> aggregatedItems.firstOrNull { it != null } }
			}
}
