package com.lasthopesoftware.bluewater;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrSession;
import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.JrFile;
import jrFileSystem.JrFiles;
import jrFileSystem.JrPlaylist;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

public class ViewPlaylists extends FragmentActivity {

	public static final String KEY = "key";   
	private JrPlaylist mPlaylist;
	
	private ProgressBar pbLoading;
	private ListView playlistView;
	
	private Context mContext;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_playlists);
        mContext = this;
        playlistView = (ListView)findViewById(R.id.lvPlaylist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingPlaylist);
        
        mPlaylist = (JrPlaylist) JrSession.selectedItem;
                
        if (mPlaylist.getSubItems().size() > 0) {
        	playlistView.setAdapter(new PlaylistAdapter(this, mPlaylist.getSubItems()));
        	playlistView.setOnItemClickListener(new ClickPlaylistListener(this, mPlaylist.getSubItems()));
        	playlistView.setOnItemLongClickListener(new BrowseItemMenu.ClickListener());
        } else {
        	playlistView.setVisibility(View.INVISIBLE);
        	pbLoading.setVisibility(View.VISIBLE);
        	JrFiles filesContainer = (JrFiles)mPlaylist.getJrFiles();
        	filesContainer.setOnFilesCompleteListener(new OnCompleteListener<List<JrFile>>() {
				
				@Override
				public void onComplete(List<JrFile> result) {
					playlistView.setAdapter(new FileListAdapter(mContext, (ArrayList<JrFile>) result));
		        	playlistView.setOnItemClickListener(new ClickFileListener(mContext, mPlaylist.getJrFiles()));
		        	
		        	playlistView.setVisibility(View.VISIBLE);
		        	pbLoading.setVisibility(View.INVISIBLE);
				}
			});
        	filesContainer.getFilesAsync();
        }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu());
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewUtils.handleNavMenuClicks(this, item)) return true;
		return super.onOptionsItemSelected(item);
	}
}