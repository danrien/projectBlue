package com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.activity.adapter.viewholder;

import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;

/**
 * Created by david on 8/23/15.
 */
public class ActiveFileDownloadsViewHolder extends RecyclerView.ViewHolder {

	public final TextView textView;
	public GetFileListItemTextTask getFileListItemTextTask;

	public ActiveFileDownloadsViewHolder(TextView textView) {
		super(textView);

		this.textView = textView;
	}
}
