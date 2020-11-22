package com.lasthopesoftware.bluewater.client.browsing.library.views

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.namehillsoftware.client.browsing.library.access.LibraryRepository
import com.namehillsoftware.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.projectblue.shared.MagicPropertyBuilder

class BrowseLibraryViewsFragment : Fragment(R.layout.tabbed_library_items_layout), IItemListMenuChangeHandler, TabLayout.OnTabSelectedListener {

	companion object {
		private val SAVED_TAB_KEY = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_TAB_KEY")
		private val SAVED_SCROLL_POS = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_SCROLL_POS")
		private val SAVED_SELECTED_VIEW = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment::class.java, "SAVED_SELECTED_VIEW")
	}

	private val lazyLibraryViewPagerAdapter = lazy {
		val viewChildPagerAdapter = LibraryViewPagerAdapter(childFragmentManager)
		viewChildPagerAdapter.setOnItemListMenuChangeHandler(this)
		viewChildPagerAdapter
	}

	private var viewAnimator: ViewAnimator? = null
	private var itemListMenuChangeHandler: IItemListMenuChangeHandler? = null
	private lateinit var viewPager: ViewPager

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewPager = view.findViewById(R.id.libraryViewPager)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		val view = view ?: return
		val context = context ?: return

		val tabbedLibraryViewsContainer = view.findViewById<RelativeLayout>(R.id.tabbedLibraryViewsContainer)

		val libraryViewsTabs = view.findViewById<TabLayout>(R.id.tabsLibraryViews)
		libraryViewsTabs.addOnTabSelectedListener(this)

		val loadingView = view.findViewById<ProgressBar>(R.id.pbLoadingTabbedItems)

		CreateVisibleLibraryView(context, savedInstanceState, libraryViewsTabs, loadingView, tabbedLibraryViewsContainer)
	}

	fun setOnItemListMenuChangeHandler(itemListMenuChangeHandler: IItemListMenuChangeHandler?) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler
	}

	override fun onAllMenusHidden() {
		itemListMenuChangeHandler?.onAllMenusHidden()
	}

	override fun onAnyMenuShown() {
		itemListMenuChangeHandler?.onAnyMenuShown()
	}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
		itemListMenuChangeHandler?.onViewChanged(viewAnimator)
	}

	override fun onTabSelected(tab: TabLayout.Tab?) {}

	override fun onTabUnselected(tab: TabLayout.Tab?) {
		LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
	}

	override fun onTabReselected(tab: TabLayout.Tab?) {}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		val viewPager = viewPager
		outState.putInt(SAVED_TAB_KEY, viewPager.currentItem)
		outState.putInt(SAVED_SCROLL_POS, viewPager.scrollY)
		selectedBrowserLibrary
			.then { library -> if (library != null) outState.putInt(SAVED_SELECTED_VIEW, library.selectedView) }
	}

	private val selectedBrowserLibrary: Promise<Library?>
		get() {
			val context = context ?: return Promise.empty()
			val selectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(context)
			val libraryProvider = LibraryRepository(context)
			val selectedLibraryId = selectedLibraryIdentifierProvider.selectedLibraryId ?: return Promise.empty()
			return libraryProvider.getLibrary(selectedLibraryId)
		}

	private inner class CreateVisibleLibraryView(
		private val context: Context,
		private val savedInstanceState: Bundle?,
		private val libraryViewsTabs: TabLayout,
		private val loadingView: ProgressBar,
		private val tabbedLibraryViewsContainer: RelativeLayout
	) : Runnable, ImmediateResponse<List<Item>, Unit> {

		private val handler = lazy { Handler(context.mainLooper) }
		private val fillVisibleViews = lazy { com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response(this, handler.value) }

		init {
			tabbedLibraryViewsContainer.visibility = View.INVISIBLE
			loadingView.visibility = View.VISIBLE
			run()
		}

		override fun run() {
			selectedBrowserLibrary
				.then { it?.let { library ->
					getInstance(context)
						.promiseSessionConnection()
						.eventually { c -> ItemProvider.provide(c, library.selectedView) }
						.eventually(fillVisibleViews.value)
						.run {
							if (savedInstanceState == null) this
							else this.eventually<Unit>(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({
								val savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1)
								if (savedSelectedView < 0 || savedSelectedView != library.selectedView) return@response

								val savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1)
								if (savedTabKey > -1) viewPager.currentItem = savedTabKey

								val savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1)
								if (savedScrollPosition > -1) viewPager.scrollY = savedScrollPosition
							}, handler.value))
						}
						.excuse(HandleViewIoException(context, this))
						.eventuallyExcuse(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response(com.namehillsoftware.projectblue.shared.exceptions.UnexpectedExceptionToasterResponse(context), handler.value))
					}
				}
		}

		override fun respond(result: List<Item>) {
			val viewChildPagerAdapter = lazyLibraryViewPagerAdapter.value
			viewChildPagerAdapter.setLibraryViews(result)

			// Set up the ViewPager with the sections adapter.
			viewPager.adapter = viewChildPagerAdapter
			libraryViewsTabs.setupWithViewPager(viewPager)
			libraryViewsTabs.visibility = if (result.size <= 1) View.GONE else View.VISIBLE
			loadingView.visibility = View.INVISIBLE
			tabbedLibraryViewsContainer.visibility = View.VISIBLE
		}
	}
}
