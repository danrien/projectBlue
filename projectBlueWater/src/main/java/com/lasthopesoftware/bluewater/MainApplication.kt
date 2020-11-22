package com.lasthopesoftware.bluewater

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.StatusPrinter
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.UpdatePlayStatsOnCompleteRegistration
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.read.StorageReadPermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestNotificationBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.request.write.StorageWritePermissionsRequestedBroadcaster
import com.lasthopesoftware.bluewater.client.connection.receivers.SessionConnectionRegistrationsMaintainer
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFilesCollection
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.scheduling.SyncSchedulingWorker.Companion.promiseIsScheduled
import com.lasthopesoftware.bluewater.client.stored.scheduling.SyncSchedulingWorker.Companion.scheduleSync
import com.lasthopesoftware.compilation.DebugFlag
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.shared.exceptions.LoggerUncaughtExceptionHandler
import org.slf4j.LoggerFactory
import java.io.File

open class MainApplication : MultiDexApplication() {
	private val notificationManagerLazy = lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val storageReadPermissionsRequestNotificationBuilderLazy = lazy { StorageReadPermissionsRequestNotificationBuilder(this) }
	private val storageWritePermissionsRequestNotificationBuilderLazy = lazy { StorageWritePermissionsRequestNotificationBuilder(this) }
	private val localBroadcastManagerLazy = lazy { LocalBroadcastManager.getInstance(this) }

	@SuppressLint("DefaultLocale")
	override fun onCreate() {
		super.onCreate()

		initializeLogging()

		val loggerUncaughtExceptionHandler = LoggerUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler(loggerUncaughtExceptionHandler)
		Promise.Rejections.setUnhandledRejectionsReceiver(loggerUncaughtExceptionHandler)

		registerAppBroadcastReceivers()

		if (!isWorkManagerInitialized) {
			WorkManager.initialize(this, Configuration.Builder().build())
			isWorkManagerInitialized = true
		}

		promiseIsScheduled(this)
			.then { isScheduled ->
				if (!isScheduled) scheduleSync(this)
			}
	}

	private fun registerAppBroadcastReceivers() {
		localBroadcastManagerLazy.value.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val libraryId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1)
				if (libraryId < 0) return

				val fileKey = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundFileKey, -1)
				if (fileKey == -1) return

				val mediaFileId = intent.getIntExtra(MediaFileUriProvider.mediaFileFoundMediaId, -1)
				if (mediaFileId == -1) return

				val mediaFilePath = intent.getStringExtra(MediaFileUriProvider.mediaFileFoundPath)
				if (mediaFilePath.isNullOrEmpty()) return

				LibraryRepository(context)
					.getLibrary(LibraryId(libraryId))
					.then { library ->
						if (library != null) {
							val storedFileAccess = StoredFileAccess(
								context,
								StoredFilesCollection(context))
							storedFileAccess.addMediaFile(library, ServiceFile(fileKey), mediaFileId, mediaFilePath)
						}
					}
			}
		}, IntentFilter(MediaFileUriProvider.mediaFileFoundEvent))

		localBroadcastManagerLazy.value.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val libraryId = intent.getIntExtra(StorageReadPermissionsRequestedBroadcaster.ReadPermissionsLibraryId, -1)
				if (libraryId < 0) return
				notificationManagerLazy.value.notify(
					336,
					storageReadPermissionsRequestNotificationBuilderLazy.value
						.buildReadPermissionsRequestNotification(libraryId))
			}
		}, IntentFilter(StorageReadPermissionsRequestedBroadcaster.ReadPermissionsNeeded))

		localBroadcastManagerLazy.value.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(context: Context, intent: Intent) {
				val libraryId = intent.getIntExtra(StorageWritePermissionsRequestedBroadcaster.WritePermissionsLibraryId, -1)
				if (libraryId < 0) return
				notificationManagerLazy.value.notify(
					396,
					storageWritePermissionsRequestNotificationBuilderLazy.value
						.buildWritePermissionsRequestNotification(libraryId))
			}
		}, IntentFilter(StorageWritePermissionsRequestedBroadcaster.WritePermissionsNeeded))

		val connectionDependentReceiverRegistrations = listOf(
			UpdatePlayStatsOnCompleteRegistration(),
			PlaybackFileStartedScrobblerRegistration(),
			PlaybackFileStoppedScrobblerRegistration(),
			PebbleFileChangedNotificationRegistration())

		localBroadcastManagerLazy.value.registerReceiver(
			SessionConnectionRegistrationsMaintainer(localBroadcastManagerLazy.value, connectionDependentReceiverRegistrations),
			IntentFilter(SessionConnection.buildSessionBroadcast))
	}

	private fun initializeLogging() {
		val lc = LoggerFactory.getILoggerFactory() as LoggerContext
		lc.reset()

		// setup LogcatAppender
		val logcatEncoder = PatternLayoutEncoder()
		logcatEncoder.context = lc
		logcatEncoder.pattern = "[%thread] %msg%n"
		logcatEncoder.start()
		val logcatAppender = LogcatAppender()
		logcatAppender.context = lc
		logcatAppender.encoder = logcatEncoder
		logcatAppender.start()
		// add the newly created appenders to the root logger;
		// qualify Logger to disambiguate from org.slf4j.Logger
		val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
		rootLogger.level = Level.WARN
		rootLogger.addAppender(logcatAppender)
		if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() && getExternalFilesDir(null) != null) {
			val asyncAppender = AsyncAppender()
			asyncAppender.context = lc
			asyncAppender.name = "ASYNC"
			val externalFilesDir = getExternalFilesDir(null)
			if (externalFilesDir != null) {
				val logDir = File(externalFilesDir.path + File.separator + "logs")
				if (!logDir.exists()) logDir.mkdirs()
				val filePle = PatternLayoutEncoder()
				filePle.pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
				filePle.context = lc
				filePle.start()

				val rollingFileAppender = RollingFileAppender<ILoggingEvent>()
				rollingFileAppender.lazy = true
				rollingFileAppender.isAppend = true
				rollingFileAppender.context = lc
				rollingFileAppender.encoder = filePle

				val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>()
				rollingPolicy.fileNamePattern = File(logDir, "%d{yyyy-MM-dd}.log").absolutePath
				rollingPolicy.maxHistory = 30
				rollingPolicy.setParent(rollingFileAppender) // parent and context required!
				rollingPolicy.context = lc
				rollingPolicy.start()
				rollingFileAppender.rollingPolicy = rollingPolicy
				rollingFileAppender.start()
				asyncAppender.addAppender(rollingFileAppender)
			}

			// UNCOMMENT TO TWEAK OPTIONAL SETTINGS
		    // excluding caller data (used for stack traces) improves appender's performance
			asyncAppender.isIncludeCallerData = !DebugFlag.getInstance().isDebugCompilation

			asyncAppender.start()
			rootLogger.addAppender(asyncAppender)
			StatusPrinter.print(lc)
		}

		val logger = LoggerFactory.getLogger(javaClass)
		logger.info("Uncaught exceptions logging to custom uncaught exception handler.")

		if (!DebugFlag.getInstance().isDebugCompilation) return

		rootLogger.level = Level.DEBUG
		logger.info("DEBUG_MODE active")
		StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork() // or .detectAll() for all detectable problems
			.penaltyLog()
			.build())
		StrictMode.setVmPolicy(VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.penaltyLog()
			.build())
	}

	companion object {
		private var isWorkManagerInitialized = false
	}
}
