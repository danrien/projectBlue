package com.lasthopesoftware.threading;

/**
 * Created by david on 8/15/15.
 */
public interface IOneParameterRunnable<TParameter> {
	void run(TParameter parameter);
}
