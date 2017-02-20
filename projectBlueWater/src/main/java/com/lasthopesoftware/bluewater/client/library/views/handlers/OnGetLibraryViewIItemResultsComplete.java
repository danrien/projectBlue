package com.lasthopesoftware.bluewater.client.library.views.handlers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.list.ItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.List;

/**
 * Created by david on 11/5/15.
 */
public abstract class OnGetLibraryViewIItemResultsComplete<T extends IItem & IFileListParameterProvider> implements OneParameterAction<List<T>> {

	private static final String PREFS_KEY = MagicPropertyBuilder.buildMagicPropertyName(OnGetLibraryViewIItemResultsComplete.class, "TUTORIAL_SHOWN");

	private static boolean wasTutorialShown;

    private final Activity activity;
    private final ListView listView;
    private final View loadingView;
    private final int position;
    private final IItemListMenuChangeHandler itemListMenuChangeHandler;
    private final ViewGroup container;
    private final StoredItemAccess storedItemAccess;
    private final Library library;

    OnGetLibraryViewIItemResultsComplete(Activity activity, ViewGroup container, ListView listView, View loadingView, int position, IItemListMenuChangeHandler itemListMenuChangeHandler, StoredItemAccess storedItemAccess, Library library) {
        this.listView = listView;
        this.activity = activity;
        this.loadingView = loadingView;
        this.position = position;
        this.itemListMenuChangeHandler = itemListMenuChangeHandler;
        this.container = container;
        this.storedItemAccess = storedItemAccess;
        this.library = library;
    }

    @Override
    public void runWith(List<T> result) {
        if (result == null) return;

        listView.setOnItemLongClickListener(new LongClickViewAnimatorListener());
        listView.setAdapter(new ItemListAdapter<>(activity, R.id.tvStandard, result, itemListMenuChangeHandler, storedItemAccess, library));
        loadingView.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.VISIBLE);

        if (position == 0) buildTutorialView(activity, container, listView);
    }

    private final static boolean DEBUGGING_TUTORIAL = false;

	private static void buildTutorialView(final Activity activity, final ViewGroup container, final ListView listView) {
        // use this flag to ensure the least amount of possible work is done for this tutorial
        if (wasTutorialShown) return;
        wasTutorialShown = true;

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!DEBUGGING_TUTORIAL && sharedPreferences.getBoolean(PREFS_KEY, false)) return;

        int[] position = new int[2];
        container.getLocationOnScreen(position);

        final View childView = listView.getAdapter().getView(0, null, listView);
        childView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        final int measuredHeight = childView.getMeasuredHeight();

        // Put the view on the second item to make it clear we're talking about menu items
        final int topPosition = position[1] + measuredHeight + (measuredHeight / 2);
        new ShowcaseView.Builder(activity)
                .setTarget(new PointTarget(position[0], topPosition))
                .hideOnTouchOutside()
                .setContentTitle(R.string.title_long_click_menu)
                .setContentText(R.string.tutorial_long_click_menu)
                .build()
                .setBackgroundColor(activity.getResources().getColor(R.color.overlay_dark));

        sharedPreferences.edit().putBoolean(PREFS_KEY, true).apply();
    }
}
