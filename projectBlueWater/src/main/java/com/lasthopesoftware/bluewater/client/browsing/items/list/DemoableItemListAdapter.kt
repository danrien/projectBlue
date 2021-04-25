package com.lasthopesoftware.bluewater.client.browsing.items.list;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.IItem;
import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;

import java.util.List;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

public class DemoableItemListAdapter<T extends IItem> extends ItemListAdapter {

	private static final String PREFS_KEY = MagicPropertyBuilder.buildMagicPropertyName(DemoableItemListAdapter.class, "TUTORIAL_SHOWN");
	private static final boolean DEBUGGING_TUTORIAL = false;
	private final Activity activity;
	private final List<Item> items;

	private boolean wasTutorialShown;

	public DemoableItemListAdapter(Activity activity, int resource, List<Item> items, IFileListParameterProvider fileListParameterProvider, IItemListMenuChangeHandler itemListMenuEvents, StoredItemAccess storedItemAccess, Library library) {
		super(activity, resource, items, fileListParameterProvider, itemListMenuEvents, storedItemAccess, library);

		this.activity = activity;
		this.items = items;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		if (items.isEmpty() || (items.size() > 1 && position != 2)) return view;

		buildTutorialView(view);
		return view;
	}

	private void buildTutorialView(final View view) {
		// use this flag to ensure the least amount of possible work is done for this tutorial
		if (wasTutorialShown) return;
		wasTutorialShown = true;

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
		if (!DEBUGGING_TUTORIAL && sharedPreferences.getBoolean(PREFS_KEY, false)) return;

		final int displayColor = activity.getResources().getColor(R.color.clearstream_blue);

		final TourGuide tourGuide =
			TourGuide.init(activity).with(TourGuide.Technique.CLICK)
				.setPointer(new Pointer().setColor(displayColor))
				.setToolTip(new ToolTip()
					.setTitle(activity.getString(R.string.title_long_click_menu))
					.setDescription(activity.getString(R.string.tutorial_long_click_menu))
					.setBackgroundColor(displayColor))
				.setOverlay(new Overlay())
				.playOn(view);

		view.setOnLongClickListener(v -> {
			tourGuide.cleanUp();
			view.setOnLongClickListener(null);
			return false;
		});

		sharedPreferences.edit().putBoolean(PREFS_KEY, true).apply();
	}
}
