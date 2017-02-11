package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.vedsoft.fluent.FluentSpecifiedTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
abstract class AbstractProvider<T> extends FluentSpecifiedTask<String, Void, T> {

	private final IConnectionProvider connectionProvider;
	private static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	AbstractProvider(IConnectionProvider connectionProvider, String... params) {
		super(providerExecutor, params);

		this.connectionProvider = connectionProvider;
	}

	@Override
	protected final T executeInBackground(String[] params) {
		return getData(connectionProvider, params);
	}

	protected abstract T getData(IConnectionProvider connectionProvider, String[] params);
}