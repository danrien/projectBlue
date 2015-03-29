package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListActivity;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.playlists.Playlist;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.IDataTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask;

import java.io.IOException;

public class ItemMenu {
	private static class ViewHolder {
		public ViewHolder(TextView textView, ImageButton shuffleButton, ImageButton playButton, ImageButton viewButton) {
			this.textView = textView;
			this.shuffleButton = shuffleButton;
			this.playButton = playButton;
			this.viewButton = viewButton;
		}

        public final TextView textView;
        public final ImageButton shuffleButton;
        public final ImageButton playButton;
        public final ImageButton viewButton;
	}

	public static View getView(IItem item, View convertView, ViewGroup parent) {
		if (convertView == null) {
		
			final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			
			convertView = new ViewFlipper(parent.getContext());
			final ViewFlipper parentView = (ViewFlipper) convertView;
            parentView.setLayoutParams(lp);
			
	        final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        final LinearLayout rl = (LinearLayout)inflater.inflate(R.layout.layout_list_item, parentView, false);
	        final TextView textView = (TextView)rl.findViewById(R.id.tvListItem);
	        parentView.addView(rl);
	        
	        final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, parentView, false);
	        final ImageButton shuffleButton = (ImageButton)fileMenu.findViewById(R.id.btnShuffle);
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlayAll);
	        final ImageButton viewButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFiles);

			parentView.addView(fileMenu);
			
			convertView.setTag(new ViewHolder(textView, shuffleButton, playButton, viewButton));
		}
		
		if (((ViewFlipper)convertView).getDisplayedChild() != 0) ((ViewFlipper)convertView).showPrevious();
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		viewHolder.textView.setText(item.getValue());
		viewHolder.shuffleButton.setOnClickListener(new ShuffleClickHandler((IFilesContainer)item));
		viewHolder.playButton.setOnClickListener(new PlayClickHandler((IFilesContainer)item));
		viewHolder.viewButton.setOnClickListener(new ViewFilesClickHandler(item));

		return convertView;
	}
	
	private static class PlayClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public PlayClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(final View v) {
			mItem.getFiles().getFileStringList(new OnGetFileStringListCompleteListener(v.getContext()), new OnGetFileStringListErrorListener(v, this));
		}
	}
	
	private static class ShuffleClickHandler implements OnClickListener {
		private IFilesContainer mItem;
		
		public ShuffleClickHandler(IFilesContainer item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
			mItem.getFiles().getFileStringList(Files.GET_SHUFFLED, new OnGetFileStringListCompleteListener(v.getContext()), new OnGetFileStringListErrorListener(v, this));
		}
	}
	
	private static class ViewFilesClickHandler implements OnClickListener {
		private IItem mItem;
		
		public ViewFilesClickHandler(IItem item) {
			mItem = item;
		}
		
		@Override
		public void onClick(View v) {
    		Intent intent = new Intent(v.getContext(), FileListActivity.class);
    		intent.setAction(mItem instanceof Playlist ? FileListActivity.VIEW_PLAYLIST_FILES : FileListActivity.VIEW_ITEM_FILES);
    		intent.putExtra(FileListActivity.KEY, mItem.getKey());
    		v.getContext().startActivity(intent);
		}
	}
	
	private static class OnGetFileStringListCompleteListener implements OnCompleteListener<String> {
		private final Context mContext;
		
		public OnGetFileStringListCompleteListener(final Context context) {
			mContext = context;
		}
		
		@Override
		public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
			PlaybackService.launchMusicService(mContext, result);
		}
		
	}
	
	private static class OnGetFileStringListErrorListener implements OnErrorListener<String> {
		private final View mView;
		private final OnClickListener mOnClickListener;
		
		public OnGetFileStringListErrorListener(final View view, final OnClickListener onClickListener) {
			mView = view;
			mOnClickListener = onClickListener;
		}		
		
		@Override
		public boolean onError(ISimpleTask<String, Void, String> owner, boolean isHandled, Exception innerException) {
			if (innerException instanceof IOException) {
				PollConnection.Instance.get(mView.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						mOnClickListener.onClick(mView);
					}
				});
				
				WaitForConnectionDialog.show(mView.getContext());
				return true;
			}
			return false;
		}
		
	}
}
