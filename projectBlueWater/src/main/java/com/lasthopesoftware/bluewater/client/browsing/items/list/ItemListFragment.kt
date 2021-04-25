package com.lasthopesoftware.bluewater.client.browsing.items.list;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.fragment.app.Fragment;

import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.views.handlers.OnGetLibraryViewItemResultsComplete;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemListFragment extends Fragment {

	private static final String ARG_CATEGORY_POSITION = "category_position";

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	private CreateAndHold<ListView> lazyListView = new AbstractSynchronousLazy<ListView>() {
		@Override
		protected ListView create() {
			final ListView listView = new ListView(getActivity());
			listView.setVisibility(View.INVISIBLE);
			return listView;
		}
	};

	private CreateAndHold<ProgressBar> lazyProgressBar = new AbstractSynchronousLazy<ProgressBar>() {
		@Override
		protected ProgressBar create() {
			final ProgressBar pbLoading = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleLarge);
			final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			pbLoading.setLayoutParams(pbParams);
			return pbLoading;
		}
	};

	private CreateAndHold<RelativeLayout> lazyLayout = new AbstractSynchronousLazy<RelativeLayout>() {
		@Override
		protected RelativeLayout create() {
			final Activity activity = getActivity();

			final RelativeLayout layout = new RelativeLayout(activity);
			layout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			layout.addView(lazyProgressBar.getObject());
			layout.addView(lazyListView.getObject());

			return layout;
		}
	};

	public static ItemListFragment getPreparedFragment(final int libraryViewId) {
		final ItemListFragment returnFragment = new ItemListFragment();
		final Bundle args = new Bundle();
		args.putInt(ItemListFragment.ARG_CATEGORY_POSITION, libraryViewId);
		returnFragment.setArguments(args);
		return returnFragment;
	}

	@Override
	public View onCreateView(@NotNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
		return lazyLayout.getObject();
	}

	@Override
	public void onStart() {
		super.onStart();

		final Activity activity = getActivity();
		if (activity == null) return;

		lazyListView.getObject().setVisibility(View.INVISIBLE);
		lazyProgressBar.getObject().setVisibility(View.VISIBLE);

		final ILibraryProvider libraryProvider = new LibraryRepository(activity);
		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(activity);

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(activeLibrary -> {
				final PromisedResponse<List<Item>, Void> onGetVisibleViewsCompleteListener = LoopedInPromise.response(new VoidResponse<>(result -> {
					if (result == null || result.size() == 0) return;

					final int categoryPosition = getArguments().getInt(ARG_CATEGORY_POSITION);
					final IItem category = categoryPosition < result.size() ? result.get(categoryPosition) : result.get(result.size() - 1);

					fillStandardItemView(category);
				}), activity);

				final Runnable fillItemsRunnable = new Runnable() {

					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, activeLibrary.getSelectedView()))
							.eventually(onGetVisibleViewsCompleteListener)
							.excuse(new HandleViewIoException(activity, this))
							.eventuallyExcuse(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				fillItemsRunnable.run();
			}));
	}

	private void fillStandardItemView(final IItem category) {
		final Activity activity = getActivity();
		if (activity == null) return;

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(library -> {
				PromisedResponse<List<Item>, Void> onGetLibraryViewItemResultsComplete = LoopedInPromise.response(new OnGetLibraryViewItemResultsComplete(
					activity,
					lazyListView.getObject(),
					lazyProgressBar.getObject(),
					itemListMenuChangeHandler,
					FileListParameters.getInstance(),
					new StoredItemAccess(activity),
					library), activity);

				final Runnable fillItemsRunnable = new Runnable() {

					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, category.getKey()))
							.eventually(onGetLibraryViewItemResultsComplete)
							.excuse(new HandleViewIoException(activity, this))
							.eventuallyExcuse(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				fillItemsRunnable.run();
			}));
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}
