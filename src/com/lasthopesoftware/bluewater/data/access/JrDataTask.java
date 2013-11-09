package com.lasthopesoftware.bluewater.data.access;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.lasthopesoftware.bluewater.data.access.connection.JrConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class JrDataTask<TResult> extends SimpleTask<String, Void, TResult> implements IJrDataTask<TResult> {

	ConcurrentLinkedQueue<OnConnectListener<TResult>> onConnectListeners = new ConcurrentLinkedQueue<OnConnectListener<TResult>>();
	ArrayList<TResult> mResults;
	
	public JrDataTask() {
		super();
		super.addOnExecuteListener(new OnExecuteListener<String, Void, TResult>() {

			@Override
			public void onExecute(ISimpleTask<String, Void, TResult> owner, String... params) throws Exception {
				if (mResults == null) mResults = new ArrayList<TResult>();
				mResults.clear();
				JrConnection conn;
				conn = new JrConnection(params);
				for (OnConnectListener<TResult> workEvent : onConnectListeners)
					mResults.add(workEvent.onConnect(conn.getInputStream()));
				
				owner.setResult(mResults.get(mResults.size() - 1));
			}
		});
	}
		
	public ArrayList<TResult> getResults() {
		return mResults;
	}

	@Override
	public void addOnConnectListener(OnConnectListener<TResult> listener) {
		onConnectListeners.add(listener);
	}
	
	@Override
	public void addOnExecuteListener(OnExecuteListener<String, Void, TResult> listener) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("The OnExecuteListener operation is not supported in the Jr Data Task. Please use the OnConnectListener.");
	}

	@Override
	public void removeOnConnectListener(com.lasthopesoftware.bluewater.data.access.IJrDataTask.OnConnectListener<TResult> listener) {
		onConnectListeners.remove(listener);
	}
}
