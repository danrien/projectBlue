package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.R
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider.Companion.promiseItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.views.handlers.OnGetLibraryViewItemResultsComplete
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise.Companion.response

class PlaylistListFragment : Fragment() {
	private val lazyListView = lazy {
			val listView = ListView(activity)
			listView.visibility = View.INVISIBLE
			listView
		}

	private val lazyProgressBar = lazy {
			val pbLoading = ProgressBar(activity, null, R.attr.progressBarStyleLarge)
			val pbParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
			pbLoading.layoutParams = pbParams
			pbLoading
		}

	private val lazyLayout = lazy {
			val activity: Activity? = activity
			val layout = RelativeLayout(activity)
			layout.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			layout.addView(lazyProgressBar.value)
			layout.addView(lazyListView.value)
			layout
		}

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private var isViewHydrated = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
		lazyLayout.value

	override fun onStart() {
		super.onStart()

		if (isViewHydrated) return

		val activity = activity ?: return

		lazyListView.value.visibility = View.INVISIBLE
		lazyProgressBar.value.visibility = View.VISIBLE

		val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(activity)
		val libraryProvider = LibraryRepository(activity)
		SelectedBrowserLibraryProvider(selectedLibraryIdentifierProvider, libraryProvider)
			.browserLibrary
			.then {
				it?.let { library ->
					val listResolvedPromise = response(
						OnGetLibraryViewItemResultsComplete(
							activity,
							lazyListView.value,
							lazyProgressBar.value,
							itemListMenuChangeHandler,
							FileListParameters.getInstance(),
							StoredItemAccess(activity),
							library),
						activity)

					val playlistFillAction = object : Runnable {
						override fun run() {
							getInstance(activity).promiseSessionConnection()
								.eventually { c -> c.promiseItems(library.selectedView) }
								.eventually(listResolvedPromise)
								.then { isViewHydrated = true }
								.excuse(HandleViewIoException(activity, this))
								.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(activity), activity))
						}
					}

					playlistFillAction.run()
				}
			}
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}
}
