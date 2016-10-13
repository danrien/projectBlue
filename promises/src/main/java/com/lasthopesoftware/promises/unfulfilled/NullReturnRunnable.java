package com.lasthopesoftware.promises.unfulfilled;

import com.vedsoft.futures.callables.OneParameterCallable;
import com.vedsoft.futures.runnables.OneParameterRunnable;

/**
 * Created by david on 10/8/16.
 */
public class NullReturnRunnable<TResult> implements OneParameterCallable<TResult, Void> {
	private final OneParameterRunnable<TResult> resolve;

	NullReturnRunnable(OneParameterRunnable<TResult> resolve) {
		this.resolve = resolve;
	}

	@Override
	public Void call(TResult result) {
		resolve.run(result);
		return null;
	}
}
