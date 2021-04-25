package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Context
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

class ItemListFragment : Fragment() {

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

	private val lazyListView = lazy {
			val listView = ListView(activity)
			listView.visibility = View.INVISIBLE
			listView
		}

	private val lazyProgressBar = lazy {
			val pbLoading = ProgressBar(activity, null, android.R.attr.progressBarStyleLarge)
			val pbParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			pbParams.addRule(RelativeLayout.CENTER_IN_PARENT)
			pbLoading.layoutParams = pbParams
			pbLoading
		}

	private val lazyLayout = lazy {
			val layout = RelativeLayout(activity)
			layout.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			layout.addView(lazyProgressBar.value)
			layout.addView(lazyListView.value)
			layout
		}

	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private var isViewHydrated = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		retainInstance = true
		return lazyLayout.value
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)

		if (isViewHydrated) return

		lazyListView.value.visibility = View.INVISIBLE
		lazyProgressBar.value.visibility = View.VISIBLE

		val libraryProvider = LibraryRepository(context)
		val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(context)
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
					}, context)

					val fillItemsRunnable = object : Runnable {
						override fun run() {
							getInstance(context).promiseSessionConnection()
								.eventually { c -> c.promiseItems(activeLibrary.selectedView) }
								.eventually(onGetVisibleViewsCompleteListener)
								.excuse(HandleViewIoException(context, this))
								.eventuallyExcuse(response(UnexpectedExceptionToasterResponse(context), context))
						}
					}
					fillItemsRunnable.run()
				}
			}
	}

	private fun fillStandardItemView(category: IItem) {
		val activity = activity ?: return
		val itemListMenuChangeHandler = itemListMenuChangeHandler ?: return

		val libraryProvider = LibraryRepository(activity)
		val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(activity)
		SelectedBrowserLibraryProvider(selectedLibraryIdentifierProvider, libraryProvider)
			.browserLibrary
			.then {
				it?.let { library ->
					val onGetLibraryViewItemResultsComplete = response(OnGetLibraryViewItemResultsComplete(
						activity,
						lazyListView.value,
						lazyProgressBar.value,
						itemListMenuChangeHandler,
						FileListParameters.getInstance(),
						StoredItemAccess(activity),
						library), activity)
					val fillItemsRunnable = object : Runnable {
						override fun run() {
							getInstance(activity).promiseSessionConnection()
								.eventually { c -> c.promiseItems(category.key) }
								.eventually(onGetLibraryViewItemResultsComplete)
								.then { isViewHydrated = true }
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
}
