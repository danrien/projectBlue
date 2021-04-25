package com.lasthopesoftware.bluewater.client.browsing.items.playlists;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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

import java.util.List;

public class PlaylistListFragment extends Fragment {

	private IItemListMenuChangeHandler itemListMenuChangeHandler;

	private final CreateAndHold<ListView> lazyListView = new AbstractSynchronousLazy<ListView>() {
		@Override
		protected ListView create() {
			final ListView listView = new ListView(getActivity());
			listView.setVisibility(View.INVISIBLE);
			return listView;
		}
	};

	private final CreateAndHold<ProgressBar> lazyProgressBar = new AbstractSynchronousLazy<ProgressBar>() {
		@Override
		protected ProgressBar create() {
			final ProgressBar pbLoading = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleLarge);
			final RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			pbLoading.setLayoutParams(pbParams);
			return pbLoading;
		}
	};

	private final CreateAndHold<RelativeLayout> lazyLayout = new AbstractSynchronousLazy<RelativeLayout>() {
		@Override
		protected RelativeLayout create() {
			final Activity activity = getActivity();

			final RelativeLayout layout = new RelativeLayout(activity);
			layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			layout.addView(lazyProgressBar.getObject());
			layout.addView(lazyListView.getObject());

			return layout;
		}
	};

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return lazyLayout.getObject();
	}

	@Override
	public void onStart() {
		super.onStart();

		final FragmentActivity activity = getActivity();

		if (activity == null) return;

		lazyListView.getObject().setVisibility(View.INVISIBLE);
		lazyProgressBar.getObject().setVisibility(View.VISIBLE);

		final ISelectedLibraryIdentifierProvider selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(getContext());
		final ILibraryProvider libraryProvider = new LibraryRepository(getContext());

		libraryProvider
			.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
			.then(new VoidResponse<>(library -> {
				final PromisedResponse<List<Item>, Void> listResolvedPromise =
					LoopedInPromise.response(
						new OnGetLibraryViewItemResultsComplete(
							activity,
							lazyListView.getObject(),
							lazyProgressBar.getObject(),
							itemListMenuChangeHandler,
							FileListParameters.getInstance(),
							new StoredItemAccess(activity),
							library), activity);

				final Runnable playlistFillAction = new Runnable() {
					@Override
					public void run() {
						SessionConnection.getInstance(activity).promiseSessionConnection()
							.eventually(c -> ItemProvider.provide(c, library.getSelectedView()))
							.eventually(listResolvedPromise)
							.excuse(new HandleViewIoException(activity, this))
							.eventuallyExcuse(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(activity), activity));
					}
				};

				playlistFillAction.run();
			}));
	}

	public void setOnItemListMenuChangeHandler(IItemListMenuChangeHandler itemListMenuChangeHandler) {
		this.itemListMenuChangeHandler = itemListMenuChangeHandler;
	}
}
