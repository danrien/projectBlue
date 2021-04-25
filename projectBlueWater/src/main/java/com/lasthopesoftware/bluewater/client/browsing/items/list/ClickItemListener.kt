package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider.Companion.promiseItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListActivity.Companion.startFileListActivity
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import org.joda.time.Duration
import org.slf4j.LoggerFactory

class ClickItemListener(private val items: List<Item>, private val loadingView: View) : OnItemClickListener {
	override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		parent.visibility = ViewUtils.getVisibility(false)
		loadingView.visibility = ViewUtils.getVisibility(true)
		val item = items[position]
		val context = view.context
		getInstance(context).promiseSessionConnection()
			.eventually { c -> c.promiseItems(item.key) }
			.then({ items ->
				if (items.isNotEmpty()) {
					val itemListIntent = Intent(context, ItemListActivity::class.java)
					itemListIntent.putExtra(ItemListActivity.KEY, item.key)
					itemListIntent.putExtra(ItemListActivity.VALUE, item.value)
					context.startActivity(itemListIntent)
					return@then
				}
				startFileListActivity(context, item)
			}, { e -> logger.error("An error occurred getting nested items for item " + item.key, e) })
			.eventually {
				LoopedInPromise({
					parent.visibility = ViewUtils.getVisibility(true)
					loadingView.visibility = ViewUtils.getVisibility(false)
				}, context, Duration.standardSeconds(1))
			}
	}

	companion object {
		private val logger = LoggerFactory.getLogger(ClickItemListener::class.java)
	}
}
