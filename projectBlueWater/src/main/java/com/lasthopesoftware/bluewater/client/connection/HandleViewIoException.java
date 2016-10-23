package com.lasthopesoftware.bluewater.client.connection;

import android.content.Context;

import com.vedsoft.fluent.IFluentTask;
import com.vedsoft.futures.callables.TwoParameterFunction;

import java.io.IOException;

public class HandleViewIoException<TParams, TProgress, TResult> implements TwoParameterFunction<IFluentTask<TParams, TProgress, TResult>, Exception, Boolean> {
	
	private final Context mContext;
	private final Runnable mOnConnectionRegainedListener;
	
	@Override
	public Boolean expectUsing(IFluentTask<TParams, TProgress, TResult> owner, Exception innerException) {
		if (!(innerException instanceof IOException)) return false;
		
		WaitForConnectionActivity.beginWaiting(mContext, mOnConnectionRegainedListener);

		return true;
	}
	
	public HandleViewIoException(final Context context, final Runnable onConnectionRegainedListener) {
		mContext = context;
		mOnConnectionRegainedListener = onConnectionRegainedListener;
	}
}
