package com.lasthopesoftware.bluewater.client.stored.sync

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileJobException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileReadException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.exceptions.StoredFileWriteException
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile
import com.lasthopesoftware.bluewater.client.stored.library.sync.ControlLibrarySyncs
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException
import com.namehillsoftware.client.browsing.library.access.ILibraryProvider
import com.namehillsoftware.projectblue.shared.MagicPropertyBuilder
import com.namehillsoftware.projectblue.shared.observables.StreamedPromise
import io.reactivex.Completable
import io.reactivex.exceptions.CompositeException
import org.slf4j.LoggerFactory

class StoredFileSynchronization(
	private val libraryProvider: ILibraryProvider,
	private val localBroadcastManager: LocalBroadcastManager,
	private val syncHandler: ControlLibrarySyncs) : SynchronizeStoredFiles {

	override fun streamFileSynchronization(): Completable {
		logger.info("Starting sync.")
		localBroadcastManager.sendBroadcast(Intent(onSyncStartEvent))
		return StreamedPromise.stream(libraryProvider.allLibraries)
			.flatMap({ library -> syncHandler.observeLibrarySync(library.libraryId) }, true)
			.flatMapCompletable({ storedFileJobStatus: StoredFileJobStatus ->
				when (storedFileJobStatus.storedFileJobState) {
					StoredFileJobState.Queued -> {
						sendStoredFileBroadcast(onFileQueuedEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					StoredFileJobState.Downloading -> {
						sendStoredFileBroadcast(onFileDownloadingEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					StoredFileJobState.Downloaded -> {
						sendStoredFileBroadcast(onFileDownloadedEvent, storedFileJobStatus.storedFile)
						Completable.complete()
					}
					else ->	Completable.complete()
				}
			}, true)
			.onErrorComplete(::handleError)
			.doOnComplete(::sendStoppedSync)
			.doOnDispose(::sendStoppedSync)
	}

	private fun handleError(e: Throwable): Boolean {
		return when (e) {
			is CompositeException -> e.exceptions.all { handleError(it) }
			is StoredFileWriteException -> {
				sendStoredFileBroadcast(onFileWriteErrorEvent, e.storedFile)
				return true
			}
			is StoredFileReadException -> {
				sendStoredFileBroadcast(onFileReadErrorEvent, e.storedFile)
				return true
			}
			is StorageCreatePathException -> true
			is StoredFileJobException -> true
			else -> false
		}
	}

	private fun sendStoppedSync() {
		localBroadcastManager.sendBroadcast(Intent(onSyncStopEvent))
	}

	private fun sendStoredFileBroadcast(action: String, storedFile: StoredFile) {
		val storedFileBroadcastIntent = Intent(action)
		storedFileBroadcastIntent.putExtra(storedFileEventKey, storedFile.id)
		localBroadcastManager.sendBroadcast(storedFileBroadcastIntent)
	}

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(StoredFileSynchronization::class.java)
		@JvmField
		val onSyncStartEvent: String = magicPropertyBuilder.buildProperty("onSyncStartEvent")
		@JvmField
		val onSyncStopEvent: String = magicPropertyBuilder.buildProperty("onSyncStopEvent")
		@JvmField
		val onFileQueuedEvent: String = magicPropertyBuilder.buildProperty("onFileQueuedEvent")
		@JvmField
		val onFileDownloadingEvent: String = magicPropertyBuilder.buildProperty("onFileDownloadingEvent")
		@JvmField
		val onFileDownloadedEvent: String = magicPropertyBuilder.buildProperty("onFileDownloadedEvent")
		@JvmField
		val onFileWriteErrorEvent: String = magicPropertyBuilder.buildProperty("onFileWriteErrorEvent")
		@JvmField
		val onFileReadErrorEvent: String = magicPropertyBuilder.buildProperty("onFileReadErrorEvent")
		@JvmField
		val storedFileEventKey: String = magicPropertyBuilder.buildProperty("storedFileEventKey")
		private val logger = LoggerFactory.getLogger(StoredFileSynchronization::class.java)
	}

}
