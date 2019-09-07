package com.lasthopesoftware.bluewater.shared.android.view;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ViewUtils {

	public static boolean buildStandardMenu(final Activity activity, final Menu menu) {
		activity.getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		
		final SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
	    final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));

		return true;
	}
	
	public static boolean handleMenuClicks(final Context context, final MenuItem item) {
		if (item.getItemId() != R.id.menu_connection_settings) return false;

		context.startActivity(new Intent(context, ApplicationSettingsActivity.class));
		return true;
	}
	
	public static boolean handleNavMenuClicks(Activity activity, MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = NavUtils.getParentActivityIntent(activity);
		        if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
		            // This activity is NOT part of this app's task, so create a new task
		            // when navigating up, with a synthesized back stack.
		            TaskStackBuilder.create(activity)
		                    // Add all of this activity's parents to the back stack
		                    .addNextIntentWithParentStack(upIntent)
		                    // Navigate up to the closest parent
		                    .startActivities();
		        } else {
		            // This activity is part of this app's task, so simply
		            // navigate up to the logical parent activity.
		            NavUtils.navigateUpTo(activity, upIntent);
		        }

				return true;
		}
		return ViewUtils.handleMenuClicks(activity, item);
		
	}

	@IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
	@Retention(RetentionPolicy.SOURCE)
	public @interface Visibility {}

	@Visibility
	public static int getVisibility(boolean isVisible) {
		return isVisible ? View.VISIBLE : View.INVISIBLE;
	}

	public static int dpToPx(Context context, int dp) {
		final float densityDpi = context.getResources().getDisplayMetrics().density;
		return (int)(dp * densityDpi + .5f);
	}

	public static int getActiveListItemTextViewStyle(boolean isActive) {
		return isActive ? Typeface.BOLD : Typeface.NORMAL;
	}
}
