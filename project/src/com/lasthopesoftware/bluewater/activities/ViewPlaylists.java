package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylists;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "com.lasthopesoftware.bluewater.activities.ViewPlaylist.key";
	private int mPlaylistId;
	private JrPlaylist mPlaylist;

	private ProgressBar pbLoading;
	private ListView playlistView;

	private Context thisContext = this;
	
	private ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> visibleViewsAsyncComplete;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_playlists);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        playlistView = (ListView)findViewById(R.id.lvPlaylist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingPlaylist);
        
        mPlaylistId = 0;
        if (savedInstanceState != null) mPlaylistId = savedInstanceState.getInt(KEY);
        if (mPlaylistId == 0) mPlaylistId = getIntent().getIntExtra(KEY, 0);
        
        visibleViewsAsyncComplete = new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, ArrayList<IJrItem<?>> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (!(exception instanceof IOException)) continue;
						
						PollConnectionTask.Instance.get().addOnCompleteListener(new ISimpleTask.OnCompleteListener<String, Void, Boolean>() {
							
							@Override
							public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
								JrSession.JrFs.getVisibleViewsAsync(visibleViewsAsyncComplete);
							}
						});
						
						PollConnectionTask.Instance.get().startPolling();
						
						thisContext.startActivity(new Intent(thisContext, WaitForConnection.class));
						break;
					}
					return;
				}
				
				if (result == null) return;
				
				for (IJrItem<?> item : result) {
					if (!item.getValue().equalsIgnoreCase("Playlist")) continue;
					
					mPlaylist = ((JrPlaylists)item).getMappedPlaylists().get(mPlaylistId);
					break;
				}
				
				BuildPlaylistView();
			}
		};
		
        JrSession.JrFs.getVisibleViewsAsync(visibleViewsAsyncComplete);
	}
	
	private void BuildPlaylistView() {
                
        if (mPlaylist.getSubItems().size() > 0) {
        	playlistView.setAdapter(new PlaylistAdapter(thisContext, R.id.tvStandard, mPlaylist.getSubItems()));
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, mPlaylist.getSubItems()));
        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
        } else {
        	playlistView.setVisibility(View.INVISIBLE);
        	pbLoading.setVisibility(View.VISIBLE);
        	JrFiles filesContainer = (JrFiles)mPlaylist.getJrFiles();
        	filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<JrFile>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<JrFile>> owner, List<JrFile> result) {
					playlistView.setAdapter(new FileListAdapter(thisContext, R.id.tvStandard, result));
		        	playlistView.setOnItemClickListener(new ClickFileListener(mPlaylist.getJrFiles()));
		        	playlistView.setOnItemLongClickListener(new LongClickFlipListener());
		        	
		        	playlistView.setVisibility(View.VISIBLE);
		        	pbLoading.setVisibility(View.INVISIBLE);
				}
			});
        	filesContainer.getFilesAsync();
        }
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
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
}