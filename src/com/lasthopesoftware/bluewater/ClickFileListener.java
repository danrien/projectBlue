package com.lasthopesoftware.bluewater;

import java.util.ArrayList;

import com.lasthopesoftware.bluewater.FileSystem.IJrItemFiles;
import com.lasthopesoftware.bluewater.FileSystem.JrFile;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ClickFileListener implements OnItemClickListener {

	private IJrItemFiles mItem;
	
	public ClickFileListener(IJrItemFiles item) {
		mItem = item;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ArrayList<JrFile> playlist = mItem.getFiles();
		JrFile file = playlist.get(position);
		StreamingMusicService.StreamMusic(view.getContext(), file, playlist);
	}

}
