package com.lasthopesoftware.bluewater.client.library.items.playlists;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ViewAnimator;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.client.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;

import java.util.List;

public class PlaylistListActivity extends AppCompatActivity implements IItemListViewContainer {

	public static final String KEY = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "key");
	public static final String VALUE = MagicPropertyBuilder.buildMagicPropertyName(PlaylistListActivity.class, "value");
	private int mPlaylistId;

	private final ILazy<ISpecificLibraryProvider> lazySpecificLibraryProvider =
		new AbstractThreadLocalLazy<ISpecificLibraryProvider>() {
			@Override
			protected ISpecificLibraryProvider initialize() throws Exception {
				return new SpecificLibraryProvider(
					new SelectedBrowserLibraryIdentifierProvider(PlaylistListActivity.this).getSelectedLibraryId(),
					new LibraryRepository(PlaylistListActivity.this));
			}
		};

	private final LazyViewFinder<ProgressBar> pbLoading = new LazyViewFinder<>(this, R.id.lvItems);
	private final LazyViewFinder<ListView> playlistView = new LazyViewFinder<>(this, R.id.pbLoadingItems);
    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.findView().setVisibility(View.INVISIBLE);
    	pbLoading.findView().setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

		final OneParameterAction<List<Playlist>> onPlaylistProviderComplete = result -> {
			if (result == null) return;

			BuildPlaylistView(result);

			playlistView.findView().setVisibility(View.VISIBLE);
			pbLoading.findView().setVisibility(View.INVISIBLE);
		};

		new PlaylistsProvider(SessionConnection.getSessionConnectionProvider(), mPlaylistId)
		        .onComplete(onPlaylistProviderComplete)
		        .onError(new HandleViewIoException<>(PlaylistListActivity.this, new Runnable() {

			        @Override
			        public void run() {
				        new PlaylistsProvider(SessionConnection.getSessionConnectionProvider(), mPlaylistId)
						        .onComplete(onPlaylistProviderComplete)
						        .onError(new HandleViewIoException<>(PlaylistListActivity.this, this))
						        .execute();
			        }
		        }))
		        .execute();

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(List<Playlist> playlist) {
		lazySpecificLibraryProvider.getObject().getLibrary()
			.then(Dispatch.toContext(VoidFunc.runningCarelessly(library -> {
				final StoredItemAccess storedItemAccess = new StoredItemAccess(this, library);
				final ItemListAdapter<Playlist> itemListAdapter = new ItemListAdapter<>(this, R.id.tvStandard, playlist, new ItemListMenuChangeHandler(this), storedItemAccess, library);

				final ListView localPlaylistView = playlistView.findView();
				localPlaylistView.setAdapter(itemListAdapter);
				localPlaylistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist));
				final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
				localPlaylistView.setOnItemLongClickListener(longClickViewAnimatorListener);
			}), this));
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(KEY, mPlaylistId);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mPlaylistId = savedInstanceState.getInt(KEY);
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