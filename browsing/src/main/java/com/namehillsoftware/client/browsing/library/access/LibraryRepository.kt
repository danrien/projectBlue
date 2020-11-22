package com.namehillsoftware.client.browsing.library.access

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.repository.InsertBuilder.Companion.fromTable
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper.Companion.databaseExecutor
import com.lasthopesoftware.bluewater.repository.UpdateBuilder
import com.namehillsoftware.client.browsing.library.access.LibraryRepository.SaveLibraryWriter
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import org.slf4j.LoggerFactory

class LibraryRepository(private val context: Context) : ILibraryStorage, ILibraryProvider {
	override fun getLibrary(libraryId: LibraryId): Promise<Library?> =
		QueuedPromise(GetLibraryWriter(context, libraryId), databaseExecutor())

	override val allLibraries: Promise<Collection<Library>>
		get() = QueuedPromise(GetAllLibrariesWriter(context), databaseExecutor())

	override fun saveLibrary(library: Library): Promise<Library> =
		QueuedPromise(SaveLibraryWriter(context, library), databaseExecutor())

	override fun removeLibrary(library: Library): Promise<Any?> =
		QueuedPromise(RemoveLibraryWriter(context, library), databaseExecutor())

	private class GetAllLibrariesWriter constructor(private val context: Context) : MessageWriter<Collection<Library>> {
		override fun prepareMessage(): Collection<Library> =
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					repositoryAccessHelper
						.mapSql("SELECT * FROM " + Library.tableName)
						.fetch(Library::class.java)
				}
			}
	}

	private class GetLibraryWriter constructor(private val context: Context, private val libraryId: LibraryId) : MessageWriter<Library?> {

		override fun prepareMessage(): Library? {
			val libraryInt = libraryId.id
			if (libraryInt < 0) return null
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginNonExclusiveTransaction().use {
					return repositoryAccessHelper
						.mapSql("SELECT * FROM " + Library.tableName + " WHERE id = @id")
						.addParameter("id", libraryInt)
						.fetchFirst(Library::class.java)
				}
			}
		}
	}

	private class SaveLibraryWriter constructor(private val context: Context, private val library: Library) : MessageWriter<Library> {
		override fun prepareMessage(): Library {
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use { closeableTransaction ->
					val isLibraryExists = library.id > -1
					val artful = repositoryAccessHelper
						.mapSql(if (isLibraryExists) libraryUpdateSql.value else libraryInsertSql.value)
						.addParameter(Library.accessCodeColumn, library.accessCode)
						.addParameter(Library.userNameColumn, library.userName)
						.addParameter(Library.passwordColumn, library.password)
						.addParameter(Library.isLocalOnlyColumn, library.isLocalOnly)
						.addParameter(Library.libraryNameColumn, library.libraryName)
						.addParameter(Library.isRepeatingColumn, library.isRepeating)
						.addParameter(Library.isWakeOnLanEnabledColumn, library.isWakeOnLanEnabled)
						.addParameter(Library.customSyncedFilesPathColumn, library.customSyncedFilesPath)
						.addParameter(Library.isSyncLocalConnectionsOnlyColumn, library.isSyncLocalConnectionsOnly)
						.addParameter(Library.isUsingExistingFilesColumn, library.isUsingExistingFiles)
						.addParameter(Library.nowPlayingIdColumn, library.nowPlayingId)
						.addParameter(Library.nowPlayingProgressColumn, library.nowPlayingProgress)
						.addParameter(Library.savedTracksStringColumn, library.savedTracksString)
						.addParameter(Library.selectedViewColumn, library.selectedView)
						.addParameter(Library.selectedViewTypeColumn, library.selectedViewType)
						.addParameter(Library.syncedFileLocationColumn, library.syncedFileLocation)
					if (isLibraryExists) artful.addParameter("id", library.id)
					val result = artful.execute()
					closeableTransaction.setTransactionSuccessful()
					if (!isLibraryExists) library.setId(result.toInt())
					logger.debug("Library saved.")
					return library
				}
			}
		}

		companion object {
			private val logger = LoggerFactory.getLogger(SaveLibraryWriter::class.java)
			private val libraryInsertSql = lazy {
				fromTable(Library.tableName)
					.addColumn(Library.accessCodeColumn)
					.addColumn(Library.userNameColumn)
					.addColumn(Library.passwordColumn)
					.addColumn(Library.isLocalOnlyColumn)
					.addColumn(Library.libraryNameColumn)
					.addColumn(Library.isRepeatingColumn)
					.addColumn(Library.isWakeOnLanEnabledColumn)
					.addColumn(Library.customSyncedFilesPathColumn)
					.addColumn(Library.isSyncLocalConnectionsOnlyColumn)
					.addColumn(Library.isUsingExistingFilesColumn)
					.addColumn(Library.nowPlayingIdColumn)
					.addColumn(Library.nowPlayingProgressColumn)
					.addColumn(Library.savedTracksStringColumn)
					.addColumn(Library.selectedViewColumn)
					.addColumn(Library.selectedViewTypeColumn)
					.addColumn(Library.syncedFileLocationColumn)
					.build()
			}

			private val libraryUpdateSql = lazy {
				UpdateBuilder
					.fromTable(Library.tableName)
					.addSetter(Library.accessCodeColumn)
					.addSetter(Library.userNameColumn)
					.addSetter(Library.passwordColumn)
					.addSetter(Library.isLocalOnlyColumn)
					.addSetter(Library.libraryNameColumn)
					.addSetter(Library.isRepeatingColumn)
					.addSetter(Library.isWakeOnLanEnabledColumn)
					.addSetter(Library.customSyncedFilesPathColumn)
					.addSetter(Library.isSyncLocalConnectionsOnlyColumn)
					.addSetter(Library.isUsingExistingFilesColumn)
					.addSetter(Library.nowPlayingIdColumn)
					.addSetter(Library.nowPlayingProgressColumn)
					.addSetter(Library.savedTracksStringColumn)
					.addSetter(Library.selectedViewColumn)
					.addSetter(Library.selectedViewTypeColumn)
					.addSetter(Library.syncedFileLocationColumn)
					.setFilter("WHERE id = @id")
					.buildQuery()
			}
		}
	}

	private class RemoveLibraryWriter constructor(private val context: Context, private val library: Library) : MessageWriter<Any?> {

		override fun prepareMessage() {
			val libraryInt = library.id
			if (libraryInt < 0) return
			RepositoryAccessHelper(context).use { repositoryAccessHelper ->
				repositoryAccessHelper.beginTransaction().use {
					repositoryAccessHelper
						.mapSql("DELETE FROM ${Library.tableName} WHERE id = @id")
						.addParameter("id", libraryInt)
						.execute()
					it.setTransactionSuccessful()
				}
			}
		}
	}
}
