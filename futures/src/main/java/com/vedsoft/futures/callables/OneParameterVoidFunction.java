package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.OneParameterAction;

/**
 * Created by david on 11/8/16.
 */

public final class OneParameterVoidFunction<ParameterOne> implements OneParameterFunction<ParameterOne, Void> {

	private final OneParameterAction<ParameterOne> action;

	public OneParameterVoidFunction(OneParameterAction<ParameterOne> action) {
		this.action = action;
	}

	@Override
	public Void expectedUsing(ParameterOne parameterOne) {
		action.runWith(parameterOne);
		return null;
	}
}