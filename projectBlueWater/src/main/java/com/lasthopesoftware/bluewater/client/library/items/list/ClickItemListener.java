package com.lasthopesoftware.bluewater.client.library.items.list;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.list.FileListActivity;

import java.util.ArrayList;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class ClickItemListener implements OnItemClickListener {

	private final ArrayList<Item> mItems;
	private final Context mContext;

	public ClickItemListener(Context context, ArrayList<Item> items) {
		mContext = context;
        mItems = items;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item item = mItems.get(position);

        ItemProvider.provide(SessionConnection.getSessionConnectionProvider(), item.getKey())
            .then(runCarelessly(items -> {
				if (items == null) return;

				if (items.size() > 0) {
					final Intent itemlistIntent = new Intent(mContext, ItemListActivity.class);
					itemlistIntent.putExtra(ItemListActivity.KEY, item.getKey());
					itemlistIntent.putExtra(ItemListActivity.VALUE, item.getValue());
					mContext.startActivity(itemlistIntent);

					return;
				}

				final Intent fileListIntent = new Intent(mContext, FileListActivity.class);
				fileListIntent.putExtra(FileListActivity.KEY, item.getKey());
				fileListIntent.putExtra(FileListActivity.VALUE, item.getValue());
				fileListIntent.setAction(FileListActivity.VIEW_ITEM_FILES);
				mContext.startActivity(fileListIntent);
			}));
	}

}
