package com.lasthopesoftware.bluewater.servers.library.items.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.IItem;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFilesContainer;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.PlayClickHandler;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.ShuffleClickHandler;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.ViewFilesClickHandler;

public final class ItemMenu {
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
        ViewFlipper parentView = (ViewFlipper)convertView;
		if (parentView == null) {
		
			final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            parentView = new ViewFlipper(parent.getContext());
            convertView = parentView;
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
		
		if (parentView.getDisplayedChild() != 0) parentView.showPrevious();
		
		final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
		viewHolder.textView.setText(item.getValue());
		viewHolder.shuffleButton.setOnClickListener(new ShuffleClickHandler(parentView, (IFilesContainer)item));
		viewHolder.playButton.setOnClickListener(new PlayClickHandler(parentView, (IFilesContainer)item));
		viewHolder.viewButton.setOnClickListener(new ViewFilesClickHandler(parentView, item));

		return convertView;
	}
}
