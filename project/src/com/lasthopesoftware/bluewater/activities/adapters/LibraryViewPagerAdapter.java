package com.lasthopesoftware.bluewater.activities.adapters;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.lasthopesoftware.bluewater.activities.fragments.CategoryFragment;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class LibraryViewPagerAdapter extends  FragmentStatePagerAdapter {
	private ArrayList<IItem<?>> mLibraryViews;
	private ArrayList<CategoryFragment> mFragments;
	
	public LibraryViewPagerAdapter(FragmentManager fm) {
		super(fm);
		mLibraryViews = new ArrayList<IItem<?>>();
		mFragments = new ArrayList<CategoryFragment>();
	}
		
	public void setLibraryViews(ArrayList<IItem<?>> libraryViews) {
		mLibraryViews = libraryViews;
		mFragments = new ArrayList<CategoryFragment>(libraryViews.size());
	}

	@Override
	public Fragment getItem(int i) {
		CategoryFragment returnFragment = null;
		if (mFragments.size() > i) returnFragment = mFragments.get(i);
		if (returnFragment == null) {
			returnFragment = new CategoryFragment();
			final Bundle args = new Bundle();
			args.putInt(CategoryFragment.ARG_CATEGORY_POSITION, i);
			returnFragment.setArguments(args);
			if (mFragments.size() > i) mFragments.set(i, returnFragment);
			else mFragments.add(returnFragment);
		}
		
		return returnFragment;
	}

	@Override
	public int getCount() {
		return getPages().size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
	}

	public ArrayList<IItem<?>> getPages() {
		return mLibraryViews;
	}
}
