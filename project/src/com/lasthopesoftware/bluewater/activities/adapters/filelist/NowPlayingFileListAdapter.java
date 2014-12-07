package com.lasthopesoftware.bluewater.activities.adapters.filelist;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.filelist.listeners.PlayClickListener;
import com.lasthopesoftware.bluewater.activities.adapters.filelist.listeners.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.activities.adapters.filelist.viewholders.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.FilePlayer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.PlaylistController;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.services.StreamingMusicService;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter {

	private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton removeButton) {
			super(viewFileDetailsButton, playButton);
			
			this.removeButton = removeButton;
		}

		public final ImageButton removeButton;
	}
	
	public NowPlayingFileListAdapter(Context context, int resource, List<File> files) {
		super(context, resource, files);
		
	}

	@Override
	protected boolean getIsFilePlaying(int position, File file, PlaylistController playlistController, FilePlayer filePlayer) {
		return position == playlistController.getCurrentPosition();
	}

	@Override
	protected View getMenuView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_now_playing_file_item_menu, parent, false);
//	        fileMenu.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton removeButton = (ImageButton)fileMenu.findViewById(R.id.btnRemoveFromPlaylist);
//	        addButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
//	        playButton.setOnTouchListener(onSwipeListener);
	        
	        final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);
//	        viewFileDetailsButton.setOnTouchListener(onSwipeListener);
	        
	        fileMenu.setTag(new ViewHolder(viewFileDetailsButton, playButton, removeButton));
	        
	        convertView = fileMenu;
		}
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		
		final File file = getItem(position);
		viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(file));
		viewHolder.removeButton.setOnClickListener(new RemoveClickListener(position, this));
		viewHolder.playButton.setOnClickListener(new PlayClickListener(position, getFiles()));
		
		return convertView;
	}
	
	private static class RemoveClickListener implements OnClickListener {
		private final int mPosition;
		private final NowPlayingFileListAdapter mAdapter;
		
		public RemoveClickListener(final int position, final NowPlayingFileListAdapter adapter) {
			mPosition = position;
			mAdapter = adapter;
		}
		
		@Override
		public void onClick(View v) {
			final Context _context = v.getContext();
			if (StreamingMusicService.getPlaylistController() == null) 
				StreamingMusicService.resumeSavedPlaylist(_context);
			
			LibrarySession.GetLibrary(_context, new OnCompleteListener<Integer, Void, Library>() {

				@Override
				public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
					if (result == null) return;
					
					String newFileString = Files.serializeFileStringList(StreamingMusicService.getPlaylistController().getPlaylist());					
					result.setSavedTracksString(newFileString);
					
					LibrarySession.SaveLibrary(_context, result, new OnCompleteListener<Void, Void, Library>() {
						
						@Override
						public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
							StreamingMusicService.getPlaylistController().removeFileAt(mPosition);
							mAdapter.remove(mAdapter.getItem(mPosition));
						}
					});
				}

			});
		}
	}
}
