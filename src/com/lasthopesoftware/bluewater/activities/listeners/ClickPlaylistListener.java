package com.lasthopesoftware.bluewater.activities.listeners;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.activities.ViewPlaylists;
import com.lasthopesoftware.bluewater.data.service.objects.JrPlaylist;

public class ClickPlaylistListener implements OnItemClickListener {

	private ArrayList<JrPlaylist> mPlaylists;
	private Context mContext;
	
	public ClickPlaylistListener(Context context, ArrayList<JrPlaylist> playlists) {
		mContext = context;
		mPlaylists = playlists;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent playlistIntent = new Intent(mContext, ViewPlaylists.class);
		playlistIntent.putExtra(ViewPlaylists.KEY, mPlaylists.get(position).getKey());
		mContext.startActivity(playlistIntent);
	}

}
