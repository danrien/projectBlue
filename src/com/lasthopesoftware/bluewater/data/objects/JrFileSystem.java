package com.lasthopesoftware.bluewater.data.objects;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.data.access.JrFsResponse;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class JrFileSystem extends JrItemAsyncBase<IJrItem<?>> implements IJrItem<IJrItem<?>> {
	private HashSet<IJrItem<?>> mVisibleViews;
	private int[] mVisibleViewKeys;
	
	private OnCompleteListener<List<IJrItem<?>>> mOnCompleteClientListener;
	private OnStartListener<List<IJrItem<?>>> mOnStartListener;
	private OnConnectListener<List<IJrItem<?>>> mOnConnectListener;
	private OnErrorListener<List<IJrItem<?>>> mOnErrorListener;
	
	public JrFileSystem(int... visibleViewKeys) {
		super();
		mVisibleViewKeys = visibleViewKeys;
		
		mOnConnectListener = new OnConnectListener<List<IJrItem<?>>>() {
			
			@Override
			public List<IJrItem<?>> onConnect(InputStream is) {
				ArrayList<IJrItem<?>> returnList = new ArrayList<IJrItem<?>>();
				for (JrItem item : JrFsResponse.GetItems(is))
					returnList.add(item);
				
				return returnList;
			}
		};
		//		setPages();
	}
	
	public String getSubItemUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	public ArrayList<IJrItem<?>> getVisibleViews() {
		try {
			return getVisibleViewsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
		} catch (Exception e) {
			return new ArrayList<IJrItem<?>>();
		}
	}
	
	public void getVisibleViewsAsync() {
		getVisibleViewsAsync(null);
	}
	
	public void getVisibleViewsAsync(ISimpleTask.OnCompleteListener<String, Void, ArrayList<IJrItem<?>>> onCompleteListener) {
		SimpleTask<String, Void, ArrayList<IJrItem<?>>> getViewsTask = getVisibleViewsTask();
		
		if (onCompleteListener != null) getViewsTask.addOnCompleteListener(onCompleteListener);
		
		getViewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	private SimpleTask<String, Void, ArrayList<IJrItem<?>>> getVisibleViewsTask() {
		SimpleTask<String, Void, ArrayList<IJrItem<?>>> getViewsTask = new SimpleTask<String, Void, ArrayList<IJrItem<?>>>();
				
		getViewsTask.addOnExecuteListener(new OnExecuteListener<String, Void, ArrayList<IJrItem<?>>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, ArrayList<IJrItem<?>>> owner, String... params) throws Exception {
				if (mVisibleViews == null) {
					
					List<IJrItem<?>> libraries = getSubItems();
					mVisibleViews = new HashSet<IJrItem<?>>(libraries.size());
					for (IJrItem<?> library : libraries) {
						if (mVisibleViewKeys.length < 1) {
							if (library.getValue().equalsIgnoreCase("Playlists")) {
								mVisibleViews.add(new JrPlaylists(mVisibleViews.size()));
								continue;
							}
							
							for (IJrItem<?> view : library.getSubItems())
								mVisibleViews.add(view);
							continue;
						}
						
						for (int viewKey : mVisibleViewKeys) {
							if (viewKey != library.getKey()) continue;
							
							if (library.getValue().equalsIgnoreCase("Playlists")) {
								mVisibleViews.add(new JrPlaylists(mVisibleViews.size()));
								continue;
							}
							
							for (IJrItem<?> view : library.getSubItems())
								mVisibleViews.add(view);
						}
					}
				}
				
				owner.setResult(new ArrayList<IJrItem<?>>(mVisibleViews));
			}
		});
		
		return getViewsTask;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<IJrItem<?>>> listener) {
		mOnCompleteClientListener = listener;
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<IJrItem<?>>> listener) {
		mOnStartListener = listener;
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<IJrItem<?>>> listener) {
		mOnErrorListener = listener;
	}

	@Override
	protected OnConnectListener<List<IJrItem<?>>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<IJrItem<?>>>> getOnItemsCompleteListeners() {
		LinkedList<OnCompleteListener<List<IJrItem<?>>>> listeners = new LinkedList<OnCompleteListener<List<IJrItem<?>>>>();
		if (mOnCompleteClientListener != null) listeners.add(mOnCompleteClientListener);
		return listeners;
	}

	@Override
	protected List<OnStartListener<List<IJrItem<?>>>> getOnItemsStartListeners() {
		LinkedList<OnStartListener<List<IJrItem<?>>>> listeners = new LinkedList<OnStartListener<List<IJrItem<?>>>>();
		if (mOnStartListener != null) listeners.add(mOnStartListener);
		return listeners;
	}

	@Override
	protected List<OnErrorListener<List<IJrItem<?>>>> getOnItemsErrorListeners() {
		LinkedList<OnErrorListener<List<IJrItem<?>>>> listeners = new LinkedList<OnErrorListener<List<IJrItem<?>>>>();
		if (mOnErrorListener != null) listeners.add(mOnErrorListener);
		return listeners;
	}

	@Override
	public String[] getSubItemParams() {
		return new String[] { "Browse/Children" };
	}
}

