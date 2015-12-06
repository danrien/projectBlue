package com.lasthopesoftware.threading;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.callables.IOneParameterCallable;

import java.io.InputStream;
import java.net.HttpURLConnection;

class FluentDataTask<TResult> extends FluentTask<String, Void, TResult> {

	public FluentDataTask(final ConnectionProvider connectionProvider, final IOneParameterCallable<InputStream, TResult> onConnectListener) {
		super(new OnExecuteListener<String, Void, TResult>() {

			@Override
			public TResult onExecute(IFluentTask<String, Void, TResult> owner, String... params) throws Exception {

				final HttpURLConnection conn = connectionProvider.getConnection(params);
				try {
					final InputStream is = conn.getInputStream();
					try {
						return onConnectListener.call(is);
					} finally {
						is.close();
					}
				} finally {
					conn.disconnect();
				}
			}
		});
	}
}
