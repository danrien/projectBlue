package com.lasthopesoftware.bluewater.client.servers.list

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibrarySession
import com.lasthopesoftware.bluewater.client.servers.list.listeners.EditServerClickListener
import com.lasthopesoftware.bluewater.client.servers.list.listeners.SelectServerOnClickListener
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.namehillsoftware.handoff.promises.Promise

class ServerListAdapter(private val activity: Activity, private val browserLibrarySelection: com.namehillsoftware.client.browsing.library.access.session.IBrowserLibrarySelection)
	: DeferredListAdapter<Library, ServerListAdapter.ViewHolder>(activity, LibraryDiffer) {

	private val localBroadcastManager = lazy { LocalBroadcastManager.getInstance(activity) }
	private var activeLibrary: Library? = null

	fun updateLibraries(libraries: Collection<Library>, activeLibrary: Library?): Promise<Unit> {
		this.activeLibrary = activeLibrary
		return updateListEventually(libraries.toList())
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val relativeLayout = getInflater(parent.context).inflate(R.layout.layout_server_item, parent, false) as RelativeLayout
		return ViewHolder(relativeLayout)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.update(getItem(position))
	}

	companion object {
		private fun getInflater(context: Context): LayoutInflater {
			return context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		}
	}

	inner class ViewHolder(private val parent: RelativeLayout) : RecyclerView.ViewHolder(parent) {
		private val textView = LazyViewFinder<TextView>(parent, R.id.tvServerItem)
		private val btnSelectServer = LazyViewFinder<Button>(parent, R.id.btnSelectServer)
		private val btnConfigureServer = LazyViewFinder<ImageButton>(parent, R.id.btnConfigureServer)

		private var broadcastReceiver: BroadcastReceiver? = null
		private var onAttachStateChangeListener: View.OnAttachStateChangeListener? = null

		fun update(library: Library) {
			val textView = textView.findView()
			textView.text = library.accessCode
			textView.setTypeface(null, com.namehillsoftware.projectblue.shared.android.view.ViewUtils.getActiveListItemTextViewStyle(activeLibrary != null && library.id == activeLibrary.id))

			broadcastReceiver?.run { localBroadcastManager.value.unregisterReceiver(this) }
			localBroadcastManager.value.registerReceiver(
				object : BroadcastReceiver() {
					override fun onReceive(context: Context, intent: Intent) {
						textView.setTypeface(null, com.namehillsoftware.projectblue.shared.android.view.ViewUtils.getActiveListItemTextViewStyle(library.id == intent.getIntExtra(LibrarySession.chosenLibraryInt, -1)))
					}
				}.apply { broadcastReceiver = this },
				IntentFilter(com.namehillsoftware.client.browsing.library.access.session.BrowserLibrarySelection.libraryChosenEvent))

			onAttachStateChangeListener?.run { parent.removeOnAttachStateChangeListener(this) }
			parent.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
				override fun onViewAttachedToWindow(v: View) {}
				override fun onViewDetachedFromWindow(v: View) {
					val broadcastReceiver = broadcastReceiver
					if (broadcastReceiver != null)
						localBroadcastManager.value.unregisterReceiver(broadcastReceiver)
				}
			}.apply { onAttachStateChangeListener = this })

			btnSelectServer.findView().setOnClickListener(SelectServerOnClickListener(library, browserLibrarySelection))
			btnConfigureServer.findView().setOnClickListener(EditServerClickListener(activity, library.id))
		}
	}

	private object LibraryDiffer : DiffUtil.ItemCallback<Library>() {
		override fun areItemsTheSame(oldItem: Library, newItem: Library): Boolean = oldItem.id == newItem.id

		override fun areContentsTheSame(oldItem: Library, newItem: Library): Boolean = areItemsTheSame(oldItem, newItem)
	}
}
