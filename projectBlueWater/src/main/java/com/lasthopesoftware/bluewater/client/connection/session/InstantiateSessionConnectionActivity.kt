package com.lasthopesoftware.bluewater.client.connection.session

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.namehillsoftware.lazyj.Lazy
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise

class InstantiateSessionConnectionActivity : Activity() {
	private val lblConnectionStatus = LazyViewFinder<TextView>(this, R.id.lblConnectionStatus)

	private val selectServerIntent = Lazy { Intent(this, ApplicationSettingsActivity::class.java) }

	private val browseLibraryIntent = Lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val localBroadcastManager = Lazy { LocalBroadcastManager.getInstance(this) }

	private val handler = Lazy { Handler() }

	private val buildSessionConnectionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			handleBuildStatusChange(intent.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1))
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_status)

		lblConnectionStatus.findView().setText(R.string.lbl_connecting)

		localBroadcastManager.getObject().registerReceiver(buildSessionConnectionReceiver, IntentFilter(SessionConnection.buildSessionBroadcast))
		getInstance(this)
			.promiseSessionConnection()
			.eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({ c: IConnectionProvider? ->
				if (c == null)
					launchActivityDelayed(selectServerIntent.getObject())
				else if (intent == null || START_ACTIVITY_FOR_RETURN != intent.action)
					launchActivityDelayed(browseLibraryIntent.getObject())
				else
					finish()

				Unit.toPromise()
			}, this), com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({
				launchActivityDelayed(selectServerIntent.getObject())

				Unit.toPromise()
			}, this))
			.must { localBroadcastManager.getObject().unregisterReceiver(buildSessionConnectionReceiver) }
	}

	private fun handleBuildStatusChange(status: Int) {
		lblConnectionStatus.findView().setText(when (status) {
			BuildingSessionConnectionStatus.GettingLibrary -> R.string.lbl_getting_library_details
			BuildingSessionConnectionStatus.GettingLibraryFailed -> R.string.lbl_please_connect_to_valid_server
			BuildingSessionConnectionStatus.SendingWakeSignal -> R.string.sending_wake_signal
			BuildingSessionConnectionStatus.BuildingConnection -> R.string.lbl_connecting_to_server_library
			BuildingSessionConnectionStatus.BuildingConnectionFailed -> R.string.lbl_error_connecting_try_again
			BuildingSessionConnectionStatus.BuildingSessionComplete -> R.string.lbl_connected
			else -> R.string.lbl_connecting
		})
	}

	private fun launchActivityDelayed(intent: Intent) {
		handler.getObject().postDelayed({ startActivity(intent) }, ACTIVITY_LAUNCH_DELAY.toLong())
	}

	companion object {
		const val ACTIVITY_ID = 2032
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(InstantiateSessionConnectionActivity::class.java, "START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 1500

		/*
	 * Returns true if the session needs to be restored,
	 * false if it doesn't
	 */
		@JvmStatic
		fun restoreSessionConnection(activity: Activity): Boolean {
			return when (getInstance(activity).isSessionConnectionActive()) {
				false -> {
					val intent = Intent(activity, InstantiateSessionConnectionActivity::class.java)
					intent.action = START_ACTIVITY_FOR_RETURN
					activity.startActivityForResult(intent, ACTIVITY_ID)
					true
				}
				else -> false
			}
		}

		@JvmStatic
		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, InstantiateSessionConnectionActivity::class.java))
		}
	}
}
