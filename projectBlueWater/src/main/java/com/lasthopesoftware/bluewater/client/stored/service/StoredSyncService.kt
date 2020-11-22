package com.lasthopesoftware.bluewater.client.stored.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.lasthopesoftware.bluewater.ApplicationConstants
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.broadcasts.ScanMediaFileBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.io.FileStreamWriter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider.Instance.get
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer
import com.lasthopesoftware.bluewater.client.stored.library.items.files.download.StoredFileDownloader
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaQueryCursorProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncsHandler
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup
import com.lasthopesoftware.bluewater.client.stored.service.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.service.notifications.SyncChannelProperties
import com.lasthopesoftware.bluewater.client.stored.service.receivers.SyncStartedReceiver
import com.lasthopesoftware.bluewater.client.stored.service.receivers.file.*
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.storage.FreeSpaceLookup
import com.lasthopesoftware.storage.directories.PrivateDirectoryLookup
import com.lasthopesoftware.storage.directories.PublicDirectoryLookup
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs
import com.lasthopesoftware.storage.read.permissions.FileReadPossibleArbitrator
import com.lasthopesoftware.storage.write.permissions.ExternalStorageWritePermissionsArbitratorForOs
import com.lasthopesoftware.storage.write.permissions.FileWritePossibleArbitrator
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.util.*

class StoredSyncService : Service(), PostSyncNotification {

	companion object {
		private val logger = LoggerFactory.getLogger(StoredSyncService::class.java)
		private val doSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "doSyncAction")
		private val isUninterruptedSyncSetting = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "isUninterruptedSyncSetting")
		private val cancelSyncAction = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "cancelSyncAction")
		private val lastSyncTime = MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "lastSyncTime")
		private const val notificationId = 23

		@JvmStatic
		fun doSync(context: Context) {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = doSyncAction
			safelyStartService(context, intent)
		}

		@JvmStatic
		fun doSyncUninterruptedFromUiThread(context: Context) {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = doSyncAction
			intent.putExtra(isUninterruptedSyncSetting, true)
			context.startService(intent)
		}

		fun cancelSync(context: Context) {
			context.startService(getSelfIntent(context, cancelSyncAction))
		}

		var isSyncRunning: Boolean = false
			private set

		private fun safelyStartService(context: Context, intent: Intent) {
			try {
				ContextCompat.startForegroundService(context, intent)
			} catch (e: IllegalStateException) {
				logger.warn("An illegal state exception occurred while trying to start the service", e)
			} catch (e: SecurityException) {
				logger.warn("A security exception occurred while trying to start the service", e)
			}
		}

		private fun getSelfIntent(context: Context, action: String): Intent {
			val intent = Intent(context, StoredSyncService::class.java)
			intent.action = action
			return intent
		}
	}

	private val lazySharedPreferences = lazy { PreferenceManager.getDefaultSharedPreferences(this) }

	private val onWifiStateChangedReceiver = lazy {
		object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val isSyncOnWifiOnly = lazySharedPreferences.value.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, false)
				if (isSyncOnWifiOnly && !com.namehillsoftware.projectblue.shared.IoCommon.isWifiConnected(context)) cancelSync(this@StoredSyncService)
			}
		}
	}

	private val onPowerDisconnectedReceiver = lazy {
		object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val isSyncOnPowerOnly = lazySharedPreferences.value.getBoolean(ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, false)
				if (isSyncOnPowerOnly) cancelSync(context)
			}
		}
	}

	private val lazyActiveNotificationChannelId = lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy.value)
			else NoOpChannelActivator()
		notificationChannelActivator.activateChannel(lazyChannelConfiguration.value)
	}

	private val browseLibraryIntent = lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.action = BrowserEntryActivity.showDownloadsAction
		browseLibraryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
		browseLibraryIntent
	}

	private val notificationManagerLazy = lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val lazyChannelConfiguration = lazy { SyncChannelProperties(this) }
	private val lazyBroadcastManager = lazy { LocalBroadcastManager.getInstance(this) }
	private val lazyStoredFileAccess = lazy { StoredFileAccess(this, StoredFilesCollection(this)) }
	private val lazyReadPermissionArbitratorForOs = lazy { ExternalStorageReadPermissionsArbitratorForOs(this) }
	private val lazyLibraryRepository = lazy { LibraryRepository(this) }
	private val lazyLibraryIdentifierProvider = lazy { SelectedBrowserLibraryIdentifierProvider(this) }
	private val lazyLibraryConnections = lazy { get(this)	}

	private val lazyFileProperties = lazy {
		val filePropertyCache = FilePropertyCache.getInstance()
		CachedFilePropertiesProvider(
			lazyLibraryConnections.value,
			filePropertyCache,
			FilePropertiesProvider(
				lazyLibraryConnections.value,
				filePropertyCache))
	}

	private val lazyStoredFilesSynchronization = lazy {
		val storedSyncService = this
		val storedItemAccess = StoredItemAccess(storedSyncService)
		val cursorProvider = MediaQueryCursorProvider(
			storedSyncService,
			lazyFileProperties.value)
		val storedFileUpdater = StoredFileUpdater(
			storedSyncService,
			MediaFileUriProvider(
				storedSyncService,
				cursorProvider,
				lazyReadPermissionArbitratorForOs.value,
				lazyLibraryIdentifierProvider.value,
				true),
			MediaFileIdProvider(
				cursorProvider,
				lazyReadPermissionArbitratorForOs.value),
			StoredFileQuery(storedSyncService),
			lazyLibraryRepository.value,
			lazyFileProperties.value,
			SyncDirectoryLookup(lazyLibraryRepository.value, PublicDirectoryLookup(storedSyncService), PrivateDirectoryLookup(storedSyncService), FreeSpaceLookup))
		val syncHandler = LibrarySyncsHandler(
			StoredItemServiceFileCollector(
				storedItemAccess,
				LibraryFileProvider(LibraryFileStringListProvider(lazyLibraryConnections.value)),
				FileListParameters.getInstance()),
			lazyStoredFileAccess.value,
			storedFileUpdater,
			StoredFileJobProcessor(
				StoredFileSystemFileProducer(),
				lazyStoredFileAccess.value,
				StoredFileDownloader(ServiceFileUriQueryParamsProvider(), lazyLibraryConnections.value),
				FileReadPossibleArbitrator(),
				FileWritePossibleArbitrator(),
				FileStreamWriter()))
		StoredFileSynchronization(
			lazyLibraryRepository.value,
			lazyBroadcastManager.value,
			syncHandler)
	}

	private val lazyStoredFileEventReceivers = lazy {
		val storedSyncService = this
		val storedFileDownloadingNotifier = StoredFileDownloadingNotifier(
			lazyStoredFileAccess.value,
			lazyFileProperties.value,
			storedSyncService,
			storedSyncService)
		val storedFileMediaScannerNotifier = StoredFileMediaScannerNotifier(
			lazyStoredFileAccess.value,
			ScanMediaFileBroadcaster(this))
		val storedFileReadPermissionsReceiver = StoredFileReadPermissionsReceiver(
			lazyReadPermissionArbitratorForOs.value,
			StorageReadPermissionsRequestedBroadcaster(lazyBroadcastManager.value),
			lazyStoredFileAccess.value)
		val storedFileWritePermissionsReceiver = StoredFileWritePermissionsReceiver(
			ExternalStorageWritePermissionsArbitratorForOs(this),
			StorageWritePermissionsRequestedBroadcaster(lazyBroadcastManager.value),
			lazyStoredFileAccess.value)

		arrayOf(
			storedFileDownloadingNotifier,
			storedFileMediaScannerNotifier,
			storedFileReadPermissionsReceiver,
			storedFileWritePermissionsReceiver)
	}

	private val lazySyncStartedReceiver = lazy { SyncStartedReceiver(this) }

	private val lazyWakeLock = lazy {
		val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
		powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MagicPropertyBuilder.buildMagicPropertyName(StoredSyncService::class.java, "wakeLock"))
	}

	private val lazyShowDownloadsIntent = lazy {
		PendingIntent.getActivity(
			this,
			0,
			browseLibraryIntent.value,
			0)
	}

	private val lazyCancelIntent = lazy {
		PendingIntent.getService(
			this,
			0,
			getSelfIntent(this, cancelSyncAction),
			PendingIntent.FLAG_UPDATE_CURRENT)
	}

	private val broadcastReceivers: MutableList<BroadcastReceiver> = ArrayList()

	private var synchronizationDisposable: Disposable? = null

	override fun onCreate() {
		super.onCreate()
		lazyWakeLock.value.acquire()
	}

	@Synchronized
	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

		val action = intent.action

		if (cancelSyncAction == action) {
			finish()
			return START_NOT_STICKY
		}

		if (doSyncAction != action) {
			logger.info("$action was not $doSyncAction, not starting sync")
			return START_NOT_STICKY
		}

		val isUninterruptedSync = intent.getBooleanExtra(isUninterruptedSyncSetting, false)

		if (isSyncRunning) {
			if (isUninterruptedSync) {
				if (onWifiStateChangedReceiver.isInitialized()) unregisterReceiver(onWifiStateChangedReceiver.value)
				if (onPowerDisconnectedReceiver.isInitialized()) unregisterReceiver(onPowerDisconnectedReceiver.value)
			}
			logger.info("Sync already running, not starting again")
			return START_NOT_STICKY
		}

		if (!lazyStoredFileEventReceivers.isInitialized()) {
			for (receiveStoredFileEvent in lazyStoredFileEventReceivers.value) {
				val broadcastReceiver = StoredFileBroadcastReceiver(receiveStoredFileEvent)
				if (!broadcastReceivers.add(broadcastReceiver)) continue
				lazyBroadcastManager.value.registerReceiver(
					broadcastReceiver,
					receiveStoredFileEvent.acceptedEvents().fold(IntentFilter(), { i, e ->
						i.addAction(e)
						i
					}))
			}
		}

		if (!lazySyncStartedReceiver.isInitialized()) {
			lazyBroadcastManager.value.registerReceiver(
				lazySyncStartedReceiver.value,
				lazySyncStartedReceiver.value.acceptedEvents().fold(IntentFilter(), { i, e ->
					i.addAction(e)
					i
				}))
		}

		if (!isUninterruptedSync) {
			registerReceiver(onWifiStateChangedReceiver.value, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
			registerReceiver(onPowerDisconnectedReceiver.value, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
		}

		isSyncRunning = true
		synchronizationDisposable = lazyStoredFilesSynchronization.value
			.streamFileSynchronization()
			.subscribe(::finish, ::finish)

		return START_NOT_STICKY
	}

	private fun finish(error: Throwable) {
		logger.error("An error occurred while synchronizing stored files", error)
		finish()
	}

	private fun finish() {
		lazySharedPreferences.value
			.edit()
			.putLong(lastSyncTime, DateTime.now().millis)
			.apply()
		stopSelf()
	}

	override fun onDestroy() {
		synchronizationDisposable?.dispose()
		isSyncRunning = false

		if (lazyBroadcastManager.isInitialized()) {
			while (broadcastReceivers.isNotEmpty()) {
				val receiver = broadcastReceivers.removeAt(0)
				lazyBroadcastManager.value.unregisterReceiver(receiver)
			}
		}

		if (onWifiStateChangedReceiver.isInitialized()) unregisterReceiver(onWifiStateChangedReceiver.value)
		if (onPowerDisconnectedReceiver.isInitialized()) unregisterReceiver(onPowerDisconnectedReceiver.value)
		if (lazyWakeLock.isInitialized()) lazyWakeLock.value.release()

		stopForeground(true)

		super.onDestroy()
	}

	override fun onBind(intent: Intent): IBinder? = lazyBinder.value

	private val lazyBinder = lazy { com.namehillsoftware.projectblue.shared.GenericBinder(this) }

	override fun notify(notificationText: String?) {
		val notifyBuilder = NotificationCompat.Builder(this, lazyActiveNotificationChannelId.value)
		notifyBuilder
			.setSmallIcon(R.drawable.ic_stat_water_drop_white)
			.setContentIntent(lazyShowDownloadsIntent.value)
			.addAction(0, getString(R.string.stop_sync_button), lazyCancelIntent.value)
			.setOngoing(true)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
		notifyBuilder.setContentTitle(getText(R.string.title_sync_files))
		notifyBuilder.setContentText(notificationText)
		val syncNotification = notifyBuilder.build()
		startForeground(notificationId, syncNotification)
	}
}
