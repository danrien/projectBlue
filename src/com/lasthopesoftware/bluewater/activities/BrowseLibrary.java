package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.fragments.CategoryFragment;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.service.objects.JrFileSystem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SelectedView;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class BrowseLibrary extends FragmentActivity implements ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	ListView mLvSelectViews;
	DrawerLayout mDrawerLayout;

	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		
		if (JrSession.GetLibrary(this) == null || JrSession.GetLibrary(this).getSelectedViews().size() == 0) {
			Intent intent = new Intent(this, SetConnection.class);
			startActivity(intent);
			return;
		}
		
		displayLibrary();
	}

	public void displayLibrary() {
		setContentView(R.layout.activity_browse_library);
		setTitle("Library");
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
		);
        
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mLvSelectViews = (ListView) findViewById(R.id.lvLibraryViewSelection);
		JrSession.JrFs.setOnItemsCompleteListener(new IJrDataTask.OnCompleteListener<List<IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {
				if (result == null) return;
				
				final List<IJrItem<?>> _views = result;
				
				mLvSelectViews.setAdapter(new SelectViewAdapter(mLvSelectViews.getContext(), R.layout.layout_select_views, _views));
				
				mLvSelectViews.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						JrSession.JrFs = new JrFileSystem(_views.get(position).getKey());
						displayLibrary();
					}
				});
			}
		});
		
		JrSession.JrFs.getSubItemsAsync();
		
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		
		JrSession.JrFs.getVisibleViewsAsync(new CategoriesLoadedListener(this, mSectionsPagerAdapter, mViewPager));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		return ViewUtils.handleMenuClicks(this, item);
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	private static class SectionsPagerAdapter extends  FragmentStatePagerAdapter {
		private ArrayList<IJrItem<?>> mLibraryViews;
		private ArrayList<CategoryFragment> fragments;
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}
		
		public void setLibraryViews(ArrayList<IJrItem<?>> libraryViews) {
			mLibraryViews = libraryViews;
			fragments = new ArrayList<CategoryFragment>(libraryViews.size());
		}

		@Override
		public Fragment getItem(int i) {
			if (fragments.size() <= i) {
				CategoryFragment fragment = new CategoryFragment();
				Bundle args = new Bundle();
				args.putInt(CategoryFragment.ARG_CATEGORY_POSITION, i);
				fragment.setArguments(args);
				fragments.add(fragment);
			}
			
			return fragments.get(i);
		}

		@Override
		public int getCount() {
			return getPages().size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return !mLibraryViews.get(position).getValue().isEmpty() ? mLibraryViews.get(position).getValue().toUpperCase(Locale.ENGLISH) : "";
		}

		public ArrayList<IJrItem<?>> getPages() {
			return mLibraryViews;
		}
	}

	public static class SelectedItem extends Fragment {
		private ListView mListView;
		public static final String ARG_SELECTED_POSITION = "selected_position";
		public static final String ARG_CATEGORY_POSITION = "category_position";

		public SelectedItem() {
			super();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			mListView = new ListView(getActivity());
			return mListView;
		}
	}
	
	private static class CategoriesLoadedListener implements OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> {
		BrowseLibrary mLibraryActivity;
		SectionsPagerAdapter mSectionsPagerAdapter;
		ViewPager mViewPager;
		
		public CategoriesLoadedListener(BrowseLibrary libraryActivity, SectionsPagerAdapter sectionsPagerAdapter, ViewPager viewPager) {
			mLibraryActivity = libraryActivity;
			mSectionsPagerAdapter = sectionsPagerAdapter;
			mViewPager = viewPager;
		}
		
		@Override
		public void onComplete(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, ArrayList<IJrItem<?>> result) {
			if (owner.getState() == SimpleTaskState.ERROR) {
				for (Exception exception : owner.getExceptions()) {
					if (exception instanceof IOException) {
						PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
							
							@Override
							public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
								if (result)
									mLibraryActivity.displayLibrary();
							}
						});
						PollConnectionTask.Instance.get().startPolling();
					}
				}
			}
			
			if (result == null) return;
			
			final ArrayList<IJrItem<?>> _selectedViews = result;
			
			mSectionsPagerAdapter.setLibraryViews(_selectedViews);
			
			// Set up the ViewPager with the sections adapter.
			mViewPager.setAdapter(mSectionsPagerAdapter);
			
			PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) mLibraryActivity.findViewById(R.id.tabsLibraryViews);
			tabs.setViewPager(mViewPager);
		}
	}
}
