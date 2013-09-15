package com.lasthopesoftware.bluewater.FileSystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.lasthopesoftware.bluewater.access.IJrDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.access.IJrDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.access.IJrDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.access.IJrDataTask.OnStartListener;
import com.lasthopesoftware.bluewater.access.JrFsResponse;
import com.lasthopesoftware.bluewater.access.JrSession;
import com.lasthopesoftware.threading.ISimpleTask;


public class JrItem extends JrItemAsyncBase<JrItem> implements IJrItem<JrItem>, IJrFilesContainer {
	private ArrayList<JrItem> mSubItems;
	private ArrayList<OnStartListener<List<JrItem>>> mItemStartListeners = new ArrayList<OnStartListener<List<JrItem>>>(1);
	private ArrayList<OnErrorListener<List<JrItem>>> mItemErrorListeners = new ArrayList<OnErrorListener<List<JrItem>>>(1);
	private ArrayList<OnCompleteListener<List<JrItem>>> mItemCompleteListeners;
	private JrFiles mJrFiles;
	
	private OnConnectListener<List<JrItem>> mItemConnectListener = new OnConnectListener<List<JrItem>>() {
		
		@Override
		public List<JrItem> onConnect(InputStream is) {
			return JrFsResponse.GetItems(is);
		}
	};
		
	private OnCompleteListener<List<JrItem>> mItemCompleteListener = new OnCompleteListener<List<JrItem>>() {
		
		@Override
		public void onComplete(ISimpleTask<String, Void, List<JrItem>> owner, List<JrItem> result) {
			mSubItems = (ArrayList<JrItem>) result;			
		}
	};
	
	public JrItem(int key, String value) {
		super(key, value);
	}
	
	public JrItem(int key) {
		super();
		this.setKey(key);
	}
	
	public JrItem() {
		super();
	}
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrItem>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			List<JrItem> tempSubItems = getNewSubItemsTask().execute(getSubItemParams()).get();
			mSubItems.addAll(tempSubItems);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
	}
	
	
//
//	@Override
//	public ArrayList<JrFile> getFiles(int option) {
//		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
//		returnFiles.addAll(getFiles());
//		if (option == GET_SHUFFLED) Collections.shuffle(returnFiles, new Random(new Date().getTime()));
//		
//		return returnFiles;
//	}
	
	@Override
	public IJrItemFiles getJrFiles() {
		if (mJrFiles == null) mJrFiles = new JrFiles("Browse/Files", "ID=" + String.valueOf(this.getKey()), "Fields=Key,Name");
		return mJrFiles;
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrItem>> listener) {
		if (mItemCompleteListeners == null) {
			mItemCompleteListeners = new ArrayList<OnCompleteListener<List<JrItem>>>(2);
			mItemCompleteListeners.add(mItemCompleteListener);
		}
		if (mItemCompleteListeners.size() < 2) mItemCompleteListeners.add(listener);
		else mItemCompleteListeners.set(1, listener);
	}

	@Override
	public void setOnItemsStartListener(OnStartListener<List<JrItem>> listener) {
		if (mItemStartListeners.size() < 1) mItemStartListeners.add(listener); 
		mItemStartListeners.set(0, listener);
	}
	
	@Override
	public void setOnItemsErrorListener(OnErrorListener<List<JrItem>> listener) {
		if (mItemErrorListeners.size() < 1) mItemErrorListeners.add(listener);
		mItemErrorListeners.set(0, listener);
	}

	@Override
	protected OnConnectListener<List<JrItem>> getOnItemConnectListener() {
		return mItemConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<JrItem>>> getOnItemsCompleteListeners() {
		return mItemCompleteListeners;
	}

	@Override
	protected List<OnStartListener<List<JrItem>>> getOnItemsStartListeners() {
		return (List<OnStartListener<List<JrItem>>>) mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener<List<JrItem>>> getOnItemsErrorListeners() {
		return (List<OnErrorListener<List<JrItem>>>) mItemErrorListeners;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Browse/Children", "ID=" + String.valueOf(this.getKey())};
	}
}
