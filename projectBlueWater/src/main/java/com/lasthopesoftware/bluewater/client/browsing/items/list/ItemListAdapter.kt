package com.lasthopesoftware.bluewater.client.browsing.items.list;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;

import java.util.List;

public class ItemListAdapter extends ArrayAdapter<Item> {

	private final ListItemMenuBuilder listItemMenuBuilder;

	ItemListAdapter(Activity activity, int resource, List<Item> items, IFileListParameterProvider fileListParameterProvider, IItemListMenuChangeHandler itemListMenuEvents, StoredItemAccess storedItemAccess, Library library) {
		super(activity, resource, items);

		final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
		viewChangedHandler.setOnAllMenusHidden(itemListMenuEvents);
		viewChangedHandler.setOnAnyMenuShown(itemListMenuEvents);
		viewChangedHandler.setOnViewChangedListener(itemListMenuEvents);

		listItemMenuBuilder = new ListItemMenuBuilder(storedItemAccess, library, fileListParameterProvider);
		listItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull final ViewGroup parent) {
		return listItemMenuBuilder.getView(position, getItem(position), convertView, parent);
	}
}
