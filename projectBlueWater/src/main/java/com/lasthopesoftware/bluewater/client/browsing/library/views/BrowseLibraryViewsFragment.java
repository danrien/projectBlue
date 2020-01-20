package com.lasthopesoftware.bluewater.client.browsing.library.views;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import java.util.List;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class BrowseLibraryViewsFragment extends Fragment implements IItemListMenuChangeHandler {

	private static final String SAVED_TAB_KEY = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_TAB_KEY");
	private static final String SAVED_SCROLL_POS = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SCROLL_POS");
	private static final String SAVED_SELECTED_VIEW = MagicPropertyBuilder.buildMagicPropertyName(BrowseLibraryViewsFragment.class, "SAVED_SELECTED_VIEW");

	private ViewAnimator viewAnimator;
	private IItemListMenuChangeHandler itemListMenuChangeHandler;
	private ViewPager viewPager;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final RelativeLayout tabbedItemsLayout = (RelativeLayout) inflater.inflate(R.layout.tabbed_library_items_layout, container, false);

		viewPager = tabbedItemsLayout.findViewById(R.id.libraryViewPager);
		final RelativeLayout tabbedLibraryViewsContainer = tabbedItemsLayout.findViewById(R.id.tabbedLibraryViewsContainer);
		final PagerSlidingTabStrip libraryViewsTabs = tabbedItemsLayout.findViewById(R.id.tabsLibraryViews);
		libraryViewsTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		final ProgressBar loadingView = tabbedItemsLayout.findViewById(R.id.pbLoadingTabbedItems);

		tabbedLibraryViewsContainer.setVisibility(View.INVISIBLE);
		loadingView.setVisibility(View.VISIBLE);

		final Context context = getContext();
		if (context == null) return tabbedItemsLayout;

		final Handler handler = new Handler(context.getMainLooper());

		final PromisedResponse<List<Item>, Void> onGetVisibleViewsCompleteListener =
			LoopedInPromise.response((result) -> {
				if (result == null) return null;

				final LibraryViewPagerAdapter viewChildPagerAdapter = new LibraryViewPagerAdapter(getChildFragmentManager());
				viewChildPagerAdapter.setOnItemListMenuChangeHandler(BrowseLibraryViewsFragment.this);

				viewChildPagerAdapter.setLibraryViews(result);

				// Set up the ViewPager with the sections adapter.
				viewPager.setAdapter(viewChildPagerAdapter);
				libraryViewsTabs.setViewPager(viewPager);

				libraryViewsTabs.setVisibility(result.size() <= 1 ? View.GONE : View.VISIBLE);

				loadingView.setVisibility(View.INVISIBLE);
				tabbedLibraryViewsContainer.setVisibility(View.VISIBLE);

				return null;
			}, handler);

		getSelectedBrowserLibrary()
			.then(new VoidResponse<>(activeLibrary -> {
				final Runnable fillItemsAction = new Runnable() {
					@Override
					public void run() {
						SessionConnection.getInstance(context).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, activeLibrary.getSelectedView()))
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(new HandleViewIoException(context, this))
							.excuse(forward())
							.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(context), handler));
					}
				};

				fillItemsAction.run();
			}));

		return tabbedItemsLayout;
	}


	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}

	@Override
	public void onAllMenusHidden() {
		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onAllMenusHidden();
	}

	@Override
	public void onAnyMenuShown() {
		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onAnyMenuShown();
	}

	@Override
	public void onViewChanged(ViewAnimator viewAnimator) {
		BrowseLibraryViewsFragment.this.viewAnimator = viewAnimator;

		if (itemListMenuChangeHandler != null)
			itemListMenuChangeHandler.onViewChanged(viewAnimator);
	}

	@Override
	public void onSaveInstanceState(@NonNull final Bundle outState) {
		super.onSaveInstanceState(outState);

		if (viewPager == null) return;

		outState.putInt(SAVED_TAB_KEY, viewPager.getCurrentItem());
		outState.putInt(SAVED_SCROLL_POS, viewPager.getScrollY());

		getSelectedBrowserLibrary()
			.then(new VoidResponse<>(library -> {
				if (library != null)
					outState.putInt(SAVED_SELECTED_VIEW, library.getSelectedView());
			}));
	}

	@Override
	public void onViewStateRestored(@Nullable final Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		if (savedInstanceState == null || viewPager == null) return;

		getSelectedBrowserLibrary()
			.eventually(LoopedInPromise.response(new VoidResponse<>(library -> {
				final int savedSelectedView = savedInstanceState.getInt(SAVED_SELECTED_VIEW, -1);
				if (savedSelectedView < 0 || savedSelectedView != library.getSelectedView()) return;

				final int savedTabKey = savedInstanceState.getInt(SAVED_TAB_KEY, -1);
				if (savedTabKey > -1)
					viewPager.setCurrentItem(savedTabKey);

				final int savedScrollPosition = savedInstanceState.getInt(SAVED_SCROLL_POS, -1);
				if (savedScrollPosition > -1)
					viewPager.setScrollY(savedScrollPosition);
			}), getContext()));
	}

	private Promise<Library> getSelectedBrowserLibrary() {
		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		return libraryProvider.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId());
	}
}
