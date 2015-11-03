package com.lasthopesoftware.bluewater.servers.library.items.playlists;

import android.app.Activity;
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
import com.lasthopesoftware.bluewater.servers.connection.HandleViewIoException;
import com.lasthopesoftware.bluewater.servers.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.list.IItemListViewContainer;
import com.lasthopesoftware.bluewater.servers.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.ItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying.NowPlayingFloatingActionButton;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.access.PlaylistsProvider;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import java.util.List;

public class PlaylistListActivity extends AppCompatActivity implements IItemListViewContainer {

    public static final String KEY = "com.lasthopesoftware.bluewater.servers.library.items.playlists.key";
    public static final String VALUE = "com.lasthopesoftware.bluewater.servers.library.items.playlists.value";
	private int mPlaylistId;

	private ProgressBar pbLoading;
	private ListView playlistView;
    private ViewAnimator viewAnimator;
	private NowPlayingFloatingActionButton nowPlayingFloatingActionButton;

	private Activity thisContext = this;

	@SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        playlistView = (ListView)findViewById(R.id.lvItems);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingItems);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        playlistView.setVisibility(View.INVISIBLE);
    	pbLoading.setVisibility(View.VISIBLE);

        setTitle(getIntent().getStringExtra(VALUE));

        final PlaylistsProvider playlistsProvider = new PlaylistsProvider(SessionConnection.getSessionConnectionProvider(), mPlaylistId);
        playlistsProvider.onComplete(new ISimpleTask.OnCompleteListener<Void, Void, List<Playlist>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Playlist>> owner, List<Playlist> result) {
				if (owner.getState() == SimpleTaskState.ERROR || result == null) return;
				
				BuildPlaylistView(result);
				
				playlistView.setVisibility(View.VISIBLE);
	        	pbLoading.setVisibility(View.INVISIBLE);
			}
		}).onError(new HandleViewIoException(thisContext, new OnConnectionRegainedListener() {
					
			@Override
			public void onConnectionRegained() {
				playlistsProvider.execute();
			}
		})).execute();

		nowPlayingFloatingActionButton = NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton((RelativeLayout) findViewById(R.id.rlViewItems));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnectionActivity.restoreSessionConnection(this);
	}
	
	private void BuildPlaylistView(List<Playlist> playlist) {
		final ItemListAdapter<Playlist> itemListAdapter = new ItemListAdapter<>(thisContext, R.id.tvStandard, playlist, new ItemListMenuChangeHandler(this));
        playlistView.setAdapter(itemListAdapter);
        playlistView.setOnItemClickListener(new ClickPlaylistListener(this, playlist));
        final LongClickViewAnimatorListener longClickViewAnimatorListener = new LongClickViewAnimatorListener();
        playlistView.setOnItemLongClickListener(longClickViewAnimatorListener);
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
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
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