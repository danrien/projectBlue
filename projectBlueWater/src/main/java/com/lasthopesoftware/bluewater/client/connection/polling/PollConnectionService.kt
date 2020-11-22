package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.resources.notifications.NoOpChannelActivator
import com.lasthopesoftware.resources.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.notificationchannel.NotificationChannelActivator
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.MessengerOperator
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken
import com.namehillsoftware.lazyj.CreateAndHold
import com.namehillsoftware.lazyj.Lazy
import java.util.*
import java.util.concurrent.CancellationException

class PollConnectionService : Service(), MessengerOperator<IConnectionProvider> {

	companion object {
		@JvmStatic
		fun pollSessionConnection(context: Context): Promise<IConnectionProvider> {
			return pollSessionConnection(context, false)
		}

		@JvmStatic
		fun pollSessionConnection(context: Context, withNotification: Boolean): Promise<IConnectionProvider> {
			val promiseConnectedService = Promise { m: Messenger<PollConnectionServiceConnectionHolder> ->
				context.bindService(Intent(context, PollConnectionService::class.java), object : ServiceConnection {
					override fun onServiceConnected(name: ComponentName, service: IBinder) {
						m.sendResolution(
							PollConnectionServiceConnectionHolder(
								(service as com.namehillsoftware.projectblue.shared.GenericBinder<*>).service as PollConnectionService,
								this))
					}

					override fun onServiceDisconnected(name: ComponentName) {}
				}, Context.BIND_AUTO_CREATE)
			}

			return promiseConnectedService
				.eventually { s ->
					val connectionService = s.pollConnectionService
                    connectionService.withNotification = connectionService.withNotification || withNotification
					connectionService.lazyConnectionPoller.getObject()
						.must { context.unbindService(s.serviceConnection) }
				}
		}

		private val uniqueOnConnectionLostListeners = HashSet<Runnable>()

		/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 */
		@JvmStatic
		fun addOnConnectionLostListener(listener: Runnable) {
			synchronized(uniqueOnConnectionLostListeners) { uniqueOnConnectionLostListeners.add(listener) }
		}

		@JvmStatic
		fun removeOnConnectionLostListener(listener: Runnable) {
			synchronized(uniqueOnConnectionLostListeners) { uniqueOnConnectionLostListeners.remove(listener) }
		}

		private val stopWaitingForConnectionAction = MagicPropertyBuilder.buildMagicPropertyName(
			PollConnectionService::class.java,
			"stopWaitingForConnection")
	}

	private var withNotification = false

	private val notificationId = 99
	private val lazyBinder = Lazy { com.namehillsoftware.projectblue.shared.GenericBinder(this) }
	private val lazyHandler = Lazy { Handler(mainLooper) }

	private val lazyConnectionPoller: CreateAndHold<Promise<IConnectionProvider>> = Lazy {
		for (connectionLostListener in uniqueOnConnectionLostListeners) connectionLostListener.run()
		Promise(this)
	}

	private val lazyChannelConfiguration = Lazy { SharedChannelProperties(this) }
	private val notificationManagerLazy = Lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
	private val lazyNotificationController = Lazy { NotificationsController(this, notificationManagerLazy.getObject()) }
	private val lazyNotificationsConfiguration = Lazy {
		val notificationChannelActivator =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannelActivator(notificationManagerLazy.getObject())
			else NoOpChannelActivator()

		val channelName = notificationChannelActivator.activateChannel(lazyChannelConfiguration.getObject())
		NotificationsConfiguration(channelName, notificationId)
	}

	override fun onBind(intent: Intent): IBinder? {
		return lazyBinder.getObject()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == stopWaitingForConnectionAction && lazyConnectionPoller.isCreated)
			lazyConnectionPoller.`object`.cancel()

		return START_NOT_STICKY
	}

	override fun send(messenger: Messenger<IConnectionProvider>) {
		val cancellationToken = CancellationToken()
		messenger.cancellationRequested(cancellationToken)
		pollSessionConnection(messenger, cancellationToken, 1000)
	}

	private fun pollSessionConnection(messenger: Messenger<IConnectionProvider>, cancellationToken: CancellationToken, connectionTime: Int) {
		if (cancellationToken.isCancelled) {
			messenger.sendRejection(CancellationException("Polling the session connection was cancelled"))
			return
		}

		if (withNotification) beginNotification()

		val nextConnectionTime = if (connectionTime < 32000) connectionTime * 2 else connectionTime
		getInstance(this)
			.promiseTestedSessionConnection()
			.then({
				when (it) {
					null -> lazyHandler.getObject().postDelayed(
						{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
						connectionTime.toLong())
					else -> messenger.sendResolution(it)
				}
			}, {
				lazyHandler.getObject()
					.postDelayed(
						{ pollSessionConnection(messenger, cancellationToken, nextConnectionTime) },
						connectionTime.toLong())
			})
	}

	private fun beginNotification() {
		// Add intent for canceling waiting for connection to come back
		val intent = Intent(this, PollConnectionService::class.java)
		intent.action = stopWaitingForConnectionAction

		val pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

		val notificationsConfiguration = lazyNotificationsConfiguration.getObject()

		val builder = NotificationCompat.Builder(this, notificationsConfiguration.notificationChannel)
			.setOngoing(true)
			.setContentIntent(pi)
			.setContentTitle(getText(R.string.lbl_waiting_for_connection))
			.setContentText(getText(R.string.lbl_click_to_cancel))
			.setSmallIcon(R.drawable.clearstream_logo_dark)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

		lazyNotificationController.`object`.notifyBackground(builder.build(), notificationsConfiguration.notificationId)
	}

	override fun onDestroy() {
		if (lazyNotificationController.isCreated)
			lazyNotificationController.`object`.removeAllNotifications()
		super.onDestroy()
	}

	private class PollConnectionServiceConnectionHolder(val pollConnectionService: PollConnectionService, val serviceConnection: ServiceConnection)
}
