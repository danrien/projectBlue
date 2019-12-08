package com.lasthopesoftware.bluewater.client.library.items.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ViewAnimator;

import androidx.appcompat.app.AppCompatActivity;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ISelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SelectedBrowserLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToasterResponse;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.List;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class ItemListActivity extends AppCompatActivity implements IItemListViewContainer, ImmediateResponse<List<Item>, Void> {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(ItemListActivity.class);

    public static final String KEY = magicPropertyBuilder.buildProperty("key");
    public static final String VALUE = magicPropertyBuilder.buildProperty("value");

	private final CreateAndHold<PromisedResponse<List<Item>, Void>> itemProviderComplete = new Lazy<>(() -> LoopedInPromise.response(this, this));
    private final LazyViewFinder<ListView> itemListView = new LazyViewFinder<>(this, R.id.lvItems);
    private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.pbLoadingItems);
	private final CreateAndHold<ISelectedBrowserLibraryProvider> lazySpecificLibraryProvider =
		new AbstractSynchronousLazy<ISelectedBrowserLibraryProvider>() {
			@Override
			protected ISelectedBrowserLibraryProvider create() {
				return new SelectedBrowserLibraryProvider(
					new SelectedBrowserLibraryIdentifierProvider(ItemListActivity.this),
					new LibraryRepository(ItemListActivity.this));
			}
		};

    private ViewAnimator viewAnimator;
    private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

    private int mItemId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mItemId = 0;
        if (savedInstanceState != null) mItemId = savedInstanceState.getInt(KEY);
        if (mItemId == 0) mItemId = getIntent().getIntExtra(KEY, 0);

        setTitle(getIntent().getStringExtra(VALUE));

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.rlViewItems));
    }

    @Override
	public void onStart() {
    	super.onStart();

		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.then(new VoidResponse<>(doRestore -> {
				if (!doRestore) hydrateItems();
			}));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) hydrateItems();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void hydrateItems() {
		itemListView.findView().setVisibility(View.INVISIBLE);
		pbLoading.findView().setVisibility(View.VISIBLE);

		SessionConnection.getInstance(this).promiseSessionConnection()
			.eventually(c -> {
				final ItemProvider itemProvider = new ItemProvider(c);
				return itemProvider.promiseItems(mItemId);
			})
			.eventually(itemProviderComplete.getObject())
			.excuse(new HandleViewIoException(this, this::hydrateItems))
			.excuse(forward())
			.eventually(LoopedInPromise.response(new UnexpectedExceptionToasterResponse(this), this))
			.then(new VoidResponse<>(v -> finish()));
	}

	@Override
	public Void respond(List<Item> items) {
		if (items != null) buildItemListView(items);

		return null;
	}

	private void buildItemListView(final List<Item> items) {
		lazySpecificLibraryProvider.getObject().getBrowserLibrary()
			.eventually(LoopedInPromise.response(new VoidResponse<>(library -> {
				final StoredItemAccess storedItemAccess = new StoredItemAccess(this, library);
				final ItemListAdapter itemListAdapter = new ItemListAdapter(
					this,
					R.id.tvStandard,
					items,
					FileListParameters.getInstance(),
					new ItemListMenuChangeHandler(this),
					storedItemAccess,
					library);

				final ListView localItemListView = this.itemListView.findView();
				localItemListView.setAdapter(itemListAdapter);
				localItemListView.setOnItemClickListener(new ClickItemListener(items, pbLoading.findView()));
				localItemListView.setOnItemLongClickListener(new LongClickViewAnimatorListener());

				itemListView.findView().setVisibility(View.VISIBLE);
				pbLoading.findView().setVisibility(View.INVISIBLE);
			}), this));
	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY, mItemId);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mItemId = savedInstanceState.getInt(KEY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return ViewUtils.buildStandardMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return;

        super.onBackPressed();
    }

    @Override
    public void updateViewAnimator(ViewAnimator viewAnimator) {
        this.viewAnimator = viewAnimator;
    }

    @Override
    public NowPlayingFloatingActionButton getNowPlayingFloatingActionButton() {
        return nowPlayingFloatingActionButton;
    }
}
