package com.lasthopesoftware.bluewater.servers.library.items.list;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.Item;
import com.lasthopesoftware.bluewater.servers.library.items.access.ItemProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.list.FileListActivity;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.FluentTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import java.util.ArrayList;
import java.util.List;

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
            .onComplete(new ITwoParameterRunnable<FluentTask<Void,Void,List<Item>>, List<Item>>() {
                @Override
                public void run(FluentTask<Void, Void, List<Item>> owner, List<Item> items) {
                    if (owner.getState() == SimpleTaskState.ERROR || items == null) return;

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
                }
            })
            .execute();
	}

}
