package com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.fragment.adapter.ActiveFileDownloadsAdapter
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise

class ActiveFileDownloadsFragment : Fragment() {
	private var onSyncStartedReceiver: BroadcastReceiver? = null
	private var onSyncStoppedReceiver: BroadcastReceiver? = null
	private var onFileQueuedReceiver: BroadcastReceiver? = null
	private var onFileDownloadedReceiver: BroadcastReceiver? = null
	private val localBroadcastManager = lazy { LocalBroadcastManager.getInstance(activity!!) }

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (container == null) return null

		val viewFilesLayout = inflater.inflate(R.layout.layout_downloads, container, false) as RelativeLayout
		val progressBar = viewFilesLayout.findViewById<ProgressBar>(R.id.recyclerLoadingProgress)
		val listView = viewFilesLayout.findViewById<RecyclerView>(R.id.loadedRecyclerView)
		listView.visibility = View.INVISIBLE
		progressBar.visibility = View.VISIBLE

		val context = container.context
		val activeFileDownloadsAdapter = ActiveFileDownloadsAdapter(context)
		listView.adapter = activeFileDownloadsAdapter
		val layoutManager = LinearLayoutManager(context)
		listView.layoutManager = layoutManager

		listView.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

		val libraryRepository = LibraryRepository(context)
		val selectedBrowserLibraryProvider = SelectedBrowserLibraryProvider(
			SelectedBrowserLibraryIdentifierProvider(context),
			libraryRepository)

		selectedBrowserLibraryProvider
			.browserLibrary
			.then { library ->
				val storedFileAccess = StoredFileAccess(
					context,
					StoredFilesCollection(context))

				storedFileAccess.downloadingStoredFiles
					.eventually<Unit>(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({ storedFiles ->
						val localStoredFiles = storedFiles.groupBy { sf -> sf.id }.values.map { sf -> sf.first() }.toMutableList()

						activeFileDownloadsAdapter.updateListEventually(localStoredFiles)

						onFileDownloadedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this)	}
						localBroadcastManager.value.registerReceiver(
							object : BroadcastReceiver() {
								override fun onReceive(context: Context, intent: Intent) {
									val storedFileId = intent.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1)
									localStoredFiles.removeAll {  sf -> sf.id == storedFileId }
									activeFileDownloadsAdapter.updateListEventually(localStoredFiles.toImmutableList())
								}
							}.apply { onFileDownloadedReceiver = this },
							IntentFilter(StoredFileSynchronization.onFileDownloadedEvent))

						onFileQueuedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
						localBroadcastManager.value.registerReceiver(
							object : BroadcastReceiver() {
								override fun onReceive(context: Context, intent: Intent) {
									val storedFileId = intent.getIntExtra(StoredFileSynchronization.storedFileEventKey, -1)
									if (storedFileId == -1) return
									if (localStoredFiles.any { sf -> sf.id == storedFileId }) return

									storedFileAccess
										.getStoredFile(storedFileId)
										.eventually { storedFile ->
											if (storedFile != null && storedFile.libraryId == library.id) {
												localStoredFiles.add(storedFile)
												activeFileDownloadsAdapter.updateListEventually(localStoredFiles.toImmutableList())
											} else {
												Promise.empty()
											}
										}
								}
							}.apply { onFileQueuedReceiver = this },
							IntentFilter(StoredFileSynchronization.onFileQueuedEvent))

						progressBar.visibility = View.INVISIBLE
						listView.visibility = View.VISIBLE
					}, context))
			}

		val toggleSyncButton = viewFilesLayout.findViewById<Button>(R.id.toggleSyncButton)
		val startSyncLabel = context.getText(R.string.start_sync_button)
		val stopSyncLabel = context.getText(R.string.stop_sync_button)
		toggleSyncButton.text = if (!StoredSyncService.isSyncRunning) startSyncLabel else stopSyncLabel

		onSyncStartedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }

		localBroadcastManager.value.registerReceiver(
			object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent) {
					toggleSyncButton.text = stopSyncLabel
				}
			}.apply { onSyncStartedReceiver = this },
			IntentFilter(StoredFileSynchronization.onSyncStartEvent))

		onSyncStoppedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
		localBroadcastManager.value.registerReceiver(
			object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent) {
					toggleSyncButton.text = startSyncLabel
				}
			}.apply { onSyncStoppedReceiver = this },
			IntentFilter(StoredFileSynchronization.onSyncStopEvent))

		toggleSyncButton.setOnClickListener { v-> if (StoredSyncService.isSyncRunning) StoredSyncService.cancelSync(v.context) else StoredSyncService.doSyncUninterruptedFromUiThread(v.context) }
		toggleSyncButton.isEnabled = true
		return viewFilesLayout
	}

	override fun onDestroy() {
		super.onDestroy()
		if (!localBroadcastManager.isInitialized()) return

		onSyncStartedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
		onSyncStoppedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
		onFileDownloadedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
		onFileQueuedReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
	}
}
