package com.lasthopesoftware.bluewater.settings;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.about.AboutActivity;

public class SettingsMenu {

	private final Activity activity;

	public SettingsMenu(Activity activity) {
		this.activity = activity;
	}

	public boolean buildSettingsMenu(Menu menu) {
		activity.getMenuInflater().inflate(R.menu.menu_settings, menu);
		final MenuItem menuItem = menu.findItem(R.id.menu_about_app);
		menuItem.setTitle(
			String.format(
				activity.getString(R.string.title_activity_about),
				activity.getString(R.string.app_name)));

		return true;
	}

	public boolean handleSettingsMenuClicks(final MenuItem item) {
		if (item.getItemId() != R.id.menu_about_app) return false;

		activity.startActivity(new Intent(activity, AboutActivity.class));
		return true;
	}
}
