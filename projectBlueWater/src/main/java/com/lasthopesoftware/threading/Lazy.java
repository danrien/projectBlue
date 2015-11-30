package com.lasthopesoftware.threading;

import java.util.concurrent.Callable;

/**
 * Created by david on 11/28/15.
 */
public class Lazy<T> {

	private final Callable<T> initialization;

	private T value;

	public Lazy(Callable<T> initialization) {
		this.initialization = initialization;
	}

	public T getValue() {
		return value != null ? value : getValueSynchronized();
	}

	private synchronized T getValueSynchronized() {
		if (value != null) return value;

		try {
			value = initialization.call();
		} catch (Exception exception) {
			Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
		}

		return value;
	}
}
