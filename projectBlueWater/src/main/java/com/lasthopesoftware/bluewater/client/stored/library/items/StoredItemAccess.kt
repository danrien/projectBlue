package com.lasthopesoftware.bluewater.client.stored.library.items

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper.Companion.databaseExecutor
import com.namehillsoftware.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.lazyj.Lazy

class StoredItemAccess(private val context: Context) : IStoredItemAccess {
	override fun toggleSync(libraryId: LibraryId, item: IItem, enable: Boolean) {
		val inferredItem = inferItem(item)
		if (enable) enableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
		else disableItemSync(libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
	}

	override fun isItemMarkedForSync(libraryId: LibraryId, item: IItem): Promise<Boolean> {
		return QueuedPromise(MessageWriter<Boolean> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				val inferredItem = inferItem(item)
				isItemMarkedForSync(repositoryAccessHelper, libraryId, inferredItem, StoredItemHelpers.getListType(inferredItem))
			}
		}, databaseExecutor())
	}

	override fun disableAllLibraryItems(libraryId: LibraryId): Promise<Any?> =
		QueuedPromise(MessageWriter<Any?> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql("DELETE FROM ${StoredItem.tableName} WHERE ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}")
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}, databaseExecutor())

	private fun enableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) {
		databaseExecutor().execute {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				if (isItemMarkedForSync(repositoryAccessHelper, libraryId, item, itemType)) return@execute
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql(storedItemInsertSql.getObject())
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.addParameter(StoredItem.serviceIdColumnName, item.key)
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}
	}

	private fun disableItemSync(libraryId: LibraryId, item: IItem, itemType: ItemType) {
		databaseExecutor().execute {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					repositoryAccessHelper
						.mapSql("""
							DELETE FROM ${StoredItem.tableName}
							WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
							AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
							AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
						.addParameter(StoredItem.serviceIdColumnName, item.key)
						.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
						.addParameter(StoredItem.itemTypeColumnName, itemType)
						.execute()
					closeableTransaction.setTransactionSuccessful()
				}
			}
		}
	}

	override fun promiseStoredItems(libraryId: LibraryId): Promise<Collection<StoredItem>> {
		return QueuedPromise(MessageWriter<Collection<StoredItem>> {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper
					.mapSql("SELECT * FROM ${StoredItem.tableName} WHERE ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}")
					.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
					.fetch(StoredItem::class.java)
			}
		}, databaseExecutor())
	}

	companion object {
		private val storedItemInsertSql = Lazy {
			fromTable(StoredItem.tableName)
				.addColumn(StoredItem.libraryIdColumnName)
				.addColumn(StoredItem.serviceIdColumnName)
				.addColumn(StoredItem.itemTypeColumnName)
				.build()
		}

		@JvmStatic
		private fun isItemMarkedForSync(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): Boolean {
			return getStoredItem(helper, libraryId, item, itemType) != null
		}

		private fun getStoredItem(helper: RepositoryAccessHelper, libraryId: LibraryId, item: IItem, itemType: ItemType): StoredItem? {
			return helper.mapSql("""
					SELECT * FROM ${StoredItem.tableName}
					WHERE ${StoredItem.serviceIdColumnName} = @${StoredItem.serviceIdColumnName}
					AND ${StoredItem.libraryIdColumnName} = @${StoredItem.libraryIdColumnName}
					AND ${StoredItem.itemTypeColumnName} = @${StoredItem.itemTypeColumnName}""")
				.addParameter(StoredItem.serviceIdColumnName, item.key)
				.addParameter(StoredItem.libraryIdColumnName, libraryId.id)
				.addParameter(StoredItem.itemTypeColumnName, itemType)
				.fetchFirst(StoredItem::class.java)
		}

		private fun inferItem(item: IItem): IItem {
			if (item is Item) {
				val playlist = item.playlist
				if (playlist != null) return playlist
			}
			return item
		}
	}
}
