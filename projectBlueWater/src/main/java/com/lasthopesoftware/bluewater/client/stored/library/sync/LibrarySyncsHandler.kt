package com.lasthopesoftware.bluewater.client.stored.library.sync

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.ProcessStoredFileJobs
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobStatus
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.UpdateStoredFiles
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy
import io.reactivex.Observable
import org.slf4j.LoggerFactory

class LibrarySyncsHandler(
	private val serviceFilesToSyncCollector: CollectServiceFilesForSync,
	private val storedFileAccess: IStoredFileAccess,
	private val storedFileUpdater: UpdateStoredFiles,
	private val storedFileJobsProcessor: ProcessStoredFileJobs) : ControlLibrarySyncs {

	override fun observeLibrarySync(libraryId: LibraryId): Observable<StoredFileJobStatus> {
		val promisedServiceFilesToSync = serviceFilesToSyncCollector.promiseServiceFilesToSync(libraryId)
		return com.namehillsoftware.projectblue.shared.observables.StreamedPromise.stream(Promise(MessengerOperator<Set<ServiceFile>> { messenger ->
			val cancellationProxy = CancellationProxy()
			messenger.cancellationRequested(cancellationProxy)
			cancellationProxy.doCancel(promisedServiceFilesToSync)
			promisedServiceFilesToSync
				.eventually { allServiceFilesToSync ->
					val serviceFilesSet = allServiceFilesToSync as? Set<ServiceFile> ?: allServiceFilesToSync.toSet()
					val pruneFilesTask = storedFileAccess.pruneStoredFiles(libraryId, serviceFilesSet)
					cancellationProxy.doCancel(pruneFilesTask)
					pruneFilesTask.excuse { e -> logger.warn("There was an error pruning the files", e) }
					pruneFilesTask.then { serviceFilesSet }
				}
				.then(ResolutionProxy(messenger), RejectionProxy(messenger))
		}))
		.map { serviceFile ->
			val promiseDownloadedStoredFile = storedFileUpdater
				.promiseStoredFileUpdate(libraryId, serviceFile)
				.then<StoredFileJob> { storedFile ->
					if (storedFile == null || storedFile.isDownloadComplete) null
					else StoredFileJob(libraryId, serviceFile, storedFile)
				}

			promiseDownloadedStoredFile
				.excuse { r ->
					logger.warn("An error occurred creating or updating $serviceFile", r)
				}
			promiseDownloadedStoredFile
		}
		.toList()
		.toObservable()
		.flatMap { promises ->
			val observablePromise = Promise.whenAll(promises)
				.then { storedFileJobs ->
					storedFileJobsProcessor.observeStoredFileDownload(storedFileJobs.filterNotNull())
				}
			com.namehillsoftware.projectblue.shared.observables.ObservedPromise.observe(observablePromise)
		}
		.flatMap { it }
	}

	companion object {
		private val logger = LoggerFactory.getLogger(LibrarySyncsHandler::class.java)
	}
}
