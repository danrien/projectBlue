package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractProvider<Data> {

	public static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	private final ProvideDataTask<Data> provideDataTask;

	AbstractProvider(IConnectionProvider connectionProvider, String... params) {
		provideDataTask = new ProvideDataTask<>(this, connectionProvider, params);
	}

	public final Promise<Data> promiseData() {
		return new QueuedPromise<>(provideDataTask, providerExecutor);
	}

	protected abstract Data getData(IConnectionProvider connectionProvider, CancellationToken cancellation, String[] params) throws Throwable;

	private static class ProvideDataTask<Data> implements OneParameterAction<Messenger<Data>> {
		private final AbstractProvider<Data> provider;
		private final IConnectionProvider connectionProvider;
		private final String[] params;
		private final CancellationToken cancellation = new CancellationToken();

		ProvideDataTask(AbstractProvider<Data> provider, IConnectionProvider connectionProvider, String... params) {
			this.provider = provider;
			this.connectionProvider = connectionProvider;
			this.params = params;
		}

		@Override
		public void runWith(Messenger<Data> messenger) {
			messenger.cancellationRequested(cancellation);

			try {
				messenger.sendResolution(provider.getData(connectionProvider, cancellation, params));
			} catch (Throwable e) {
				messenger.sendRejection(e);
			}
		}
	}
}