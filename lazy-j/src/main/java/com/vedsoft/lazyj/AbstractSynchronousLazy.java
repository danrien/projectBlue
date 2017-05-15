package com.vedsoft.lazyj;

public abstract class AbstractSynchronousLazy<T> implements ILazy<T> {

	private T object;

	private RuntimeException exception;

	public boolean isInitialized() {
		return object != null || exception != null;
	}

	public final T getObject() {
		return isInitialized() ? object : getValueSynchronized();
	}

	private synchronized T getValueSynchronized() {
		if (!isInitialized()) {
			try {
				object = initialize();
			} catch (Exception e) {
				exception = new RuntimeException(e);
			}
		}

		if (exception != null)
			throw exception;

		return object;
	}

	protected abstract T initialize() throws Exception;
}
