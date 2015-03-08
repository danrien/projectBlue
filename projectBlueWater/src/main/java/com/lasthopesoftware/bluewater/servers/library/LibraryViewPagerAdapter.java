package com.lasthopesoftware.bluewater.servers.library;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lasthopesoftware.bluewater.servers.library.items.IItem;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A {@link LibraryViewPagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class LibraryViewPagerAdapter extends  FragmentStatePagerAdapter {
	private ArrayList<IItem> mLibraryViews = new ArrayList<IItem>();
	
	public LibraryViewPagerAdapter(FragmentManager fm) {
		super(fm);
	}
		
	public void setLibraryViews(ArrayList<IItem> libraryViews) {
		mLibraryViews = libraryViews;
	}

	@Override
	public Fragment getItem(int i) {
        // The position correlates to the ID returned by the server at the high-level Library views
		return LibraryViewFragment.getPreparedFragment(i);
	}

	@Override
	public int getCount() {
		return mLibraryViews.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
	}
}
