package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
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
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold

class ItemListFragment : Fragment() {
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private val lazyListView: CreateAndHold<ListView> = object : AbstractSynchronousLazy<ListView>() {
		override fun create(): ListView {
			val listView = ListView(activity)
			listView.visibility = View.INVISIBLE
			return listView
		}
	}
	private val lazyProgressBar: CreateAndHold<ProgressBar> = object : AbstractSynchronousLazy<ProgressBar>() {
		override fun create(): ProgressBar {
			val pbLoading = ProgressBar(activity, null, android.R.attr.progressBarStyleLarge)
			val pbParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
			pbLoading.layoutParams = pbParams
			return pbLoading
		}
	}
	private val lazyLayout: CreateAndHold<RelativeLayout> = object : AbstractSynchronousLazy<RelativeLayout>() {
		override fun create(): RelativeLayout {
			val activity: Activity? = activity
			val layout = RelativeLayout(activity)
			layout.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			layout.addView(lazyProgressBar.getObject())
			layout.addView(lazyListView.getObject())
			return layout
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return lazyLayout.getObject()
	}

	override fun onStart() {
		super.onStart()
		val activity = activity ?: return

		lazyListView.getObject().visibility = View.INVISIBLE
		lazyProgressBar.getObject().visibility = View.VISIBLE

		val libraryProvider = LibraryRepository(activity)
		val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(activity)
		SelectedBrowserLibraryProvider(selectedLibraryIdentifierProvider, libraryProvider)
			.browserLibrary
			.then {
				it?.let { activeLibrary ->
					val onGetVisibleViewsCompleteListener = response<List<Item>, Unit>({ result ->
						if (result.isNotEmpty()) {
							val categoryPosition = requireArguments().getInt(ARG_CATEGORY_POSITION)
							val category =
								if (categoryPosition < result.size) result[categoryPosition]
								else result[result.size - 1]

							fillStandardItemView(category)
						}
					}, activity)

					val fillItemsRunnable = object : Runnable {
						override fun run() {
							getInstance(activity).promiseSessionConnection()
								.eventually { c -> c.promiseItems(activeLibrary.selectedView) }
								.eventually(onGetVisibleViewsCompleteListener)
								.excuse(HandleViewIoException(activity, this))
								.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(activity), activity))
						}
					}
					fillItemsRunnable.run()
				}
			}
	}

	private fun fillStandardItemView(category: IItem) {
		val activity = activity ?: return

		val libraryProvider = LibraryRepository(activity)
		val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(activity)
		SelectedBrowserLibraryProvider(selectedLibraryIdentifierProvider, libraryProvider)
			.browserLibrary
			.then {
				it?.let { library ->
					val onGetLibraryViewItemResultsComplete = response(OnGetLibraryViewItemResultsComplete(
						activity,
						lazyListView.getObject(),
						lazyProgressBar.getObject(),
						itemListMenuChangeHandler,
						FileListParameters.getInstance(),
						StoredItemAccess(activity),
						library), activity)
					val fillItemsRunnable = object : Runnable {
						override fun run() {
							getInstance(activity).promiseSessionConnection()
								.eventually { c -> c.promiseItems(category.key) }
								.eventually(onGetLibraryViewItemResultsComplete)
								.excuse(HandleViewIoException(activity, this))
								.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(activity), activity))
						}
					}
					fillItemsRunnable.run()
				}
			}
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	companion object {
		private const val ARG_CATEGORY_POSITION = "category_position"
		@JvmStatic
		fun getPreparedFragment(libraryViewId: Int): ItemListFragment {
			val returnFragment = ItemListFragment()
			val args = Bundle()
			args.putInt(ARG_CATEGORY_POSITION, libraryViewId)
			returnFragment.arguments = args
			return returnFragment
		}
	}
}
