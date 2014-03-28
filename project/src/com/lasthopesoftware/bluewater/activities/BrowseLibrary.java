package com.lasthopesoftware.bluewater.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.SelectViewAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.ViewChildPagerAdapter;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.data.service.access.IJrDataTask;
import com.lasthopesoftware.bluewater.data.service.access.connection.JrTestConnection;
import com.lasthopesoftware.bluewater.data.service.access.connection.PollConnectionTask;
import com.lasthopesoftware.bluewater.data.service.objects.IJrItem;
import com.lasthopesoftware.bluewater.data.session.JrSession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.SimpleTaskState;

public class BrowseLibrary extends FragmentActivity {

	private static final String SAVED_TAB_KEY = "com.lasthopesoftware.bluewater.activities.BrowseLibrary.SAVED_TAB_KEY";
	private static final String SAVED_SCROLL_POS = "com.lasthopesoftware.bluewater.activities.BrowseLibrary.SAVED_SCROLL_POS";
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	private SparseArray<ViewChildPagerAdapter> mViewChildAdapterCache = new SparseArray<ViewChildPagerAdapter>();

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private ListView mLvSelectViews;
	private DrawerLayout mDrawerLayout;

	private ActionBarDrawerToggle mDrawerToggle = null;
	
	private BrowseLibrary thisContext;
	
	private CharSequence mOldTitle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		thisContext = this;
		
		Intent selectServer = new Intent(thisContext, SelectServer.class);
		if (JrSession.GetLibrary(thisContext) == null || JrSession.GetLibrary(thisContext).getSelectedView() <= 0) {
			Toast.makeText(thisContext, "Please select a valid server", Toast.LENGTH_LONG).show();
			startActivity(selectServer);
			return;
		}
		
		if (!JrTestConnection.doTest(30000)) {
			Toast.makeText(thisContext, "There was an error connecting to the server, try again later!", Toast.LENGTH_LONG).show();
			startActivity(selectServer);
			return;
		}
		
		setContentView(R.layout.activity_browse_library);
		setTitle("Library");
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		
		mOldTitle = getTitle();
		mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
		) {
			 /** Called when a drawer has settled in a completely closed state. */
			@Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mOldTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mOldTitle = getActionBar().getTitle();
                getActionBar().setTitle("Select view");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

		};
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		
		displayLibrary();
	}

	public void displayLibrary() {		
		final Library library = JrSession.GetLibrary(thisContext);
		mLvSelectViews = (ListView) findViewById(R.id.lvLibraryViewSelection);
		JrSession.JrFs.setOnItemsCompleteListener(new IJrDataTask.OnCompleteListener<List<IJrItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<IJrItem<?>>> owner, List<IJrItem<?>> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (exception instanceof IOException) {
							
							PollConnectionTask.Instance.get().startPolling();
							
							thisContext.startActivity(new Intent(thisContext, WaitForConnection.class));
							
							PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
								
								@Override
								public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
									if (result)
										JrSession.JrFs.getSubItemsAsync();
								}
							});
							break;
						}
					}
					return;
				}
				
				if (result == null) return;
				
				final List<IJrItem<?>> _views = result;
				
				for (IJrItem<?> item : _views) {
					if (item.getKey() != JrSession.GetLibrary(thisContext).getSelectedView()) continue;
					mOldTitle = item.getValue();
					getActionBar().setTitle(mOldTitle);
					break;
				}
				
				mLvSelectViews.setAdapter(new SelectViewAdapter(mLvSelectViews.getContext(), R.layout.layout_select_views, _views));
				
				mLvSelectViews.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						mDrawerLayout.closeDrawer(Gravity.LEFT);
						mDrawerToggle.syncState();
						
						if (library.getSelectedView() == _views.get(position).getKey()) return;
						
						library.setSelectedView(_views.get(position).getKey());
						JrSession.SaveSession(thisContext);
						JrSession.JrFs.setVisibleViews(_views.get(position).getKey());
						displayLibrary();
					}
				});
			}
		});
		
		JrSession.JrFs.getSubItemsAsync();
		
		
		ViewChildPagerAdapter viewChildPagerAdapter = mViewChildAdapterCache.get(library.getSelectedView());
		if (viewChildPagerAdapter == null) {
			
			JrSession.JrFs.getVisibleViewsAsync(new OnCompleteListener<String, Void, ArrayList<IJrItem<?>>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, ArrayList<IJrItem<?>> result) {
					final OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> _this = this;
					if (owner.getState() == SimpleTaskState.ERROR) {
						for (Exception exception : owner.getExceptions()) {
							if (exception instanceof IOException) {
								PollConnectionTask.Instance.get().addOnCompleteListener(new OnCompleteListener<String, Void, Boolean>() {
									
									@Override
									public void onComplete(ISimpleTask<String, Void, Boolean> owner, Boolean result) {
										if (result)
											JrSession.JrFs.getVisibleViewsAsync(_this);
									}
								});
								PollConnectionTask.Instance.get().startPolling();
								
								thisContext.startActivity(new Intent(thisContext, WaitForConnection.class));
								break;
							}
						}
						return;
					}
					
					if (result == null) return;
					
					ViewChildPagerAdapter viewChildPagerAdapter = new ViewChildPagerAdapter(getSupportFragmentManager());
					viewChildPagerAdapter.setLibraryViews(result);
					mViewChildAdapterCache.put(library.getSelectedView(), viewChildPagerAdapter);

					// Set up the ViewPager with the sections adapter.
					setViewPagerAdapter(viewChildPagerAdapter);
				}
			});
			return;
		}
		
		setViewPagerAdapter(viewChildPagerAdapter);
	}
	
	private void setViewPagerAdapter(ViewChildPagerAdapter viewChildPagerAdapter) {
		mViewPager.setAdapter(viewChildPagerAdapter);
		((PagerSlidingTabStrip) findViewById(R.id.tabsLibraryViews)).setViewPager(mViewPager);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_blue_water, menu);
		menu.findItem(R.id.menu_view_now_playing).setVisible(ViewUtils.displayNowPlayingMenu(this));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
			return true;
		
		return ViewUtils.handleMenuClicks(this, item);
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) mDrawerToggle.syncState();
    }
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		
		if (mViewPager != null) {
			savedInstanceState.putInt(SAVED_TAB_KEY, mViewPager.getCurrentItem());
			savedInstanceState.putInt(SAVED_SCROLL_POS, mViewPager.getScrollY());
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (mViewPager != null) {
			mViewPager.setCurrentItem(savedInstanceState.getInt(SAVED_TAB_KEY));
			mViewPager.setScrollY(savedInstanceState.getInt(SAVED_SCROLL_POS));
		}
	}

	public ViewPager getViewPager() {
		return mViewPager;
	}
}
