package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.response.EventualAction;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;

import javax.net.ssl.SSLProtocolException;

import static com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.delay;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);

	private static final Duration timeoutDuration = Duration.standardMinutes(1);

	private final Object textViewUpdateSync = new Object();

	private final TextView textView;
	private final Handler handler;

	private volatile Promise<Void> promisedState = Promise.empty();
	private volatile PromisedTextViewUpdate currentlyPromisedTextViewUpdate;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public synchronized Promise<Void> promiseTextViewUpdate(ServiceFile serviceFile) {
		promisedState.cancel();

		promisedState = promisedState
			.inevitably(new EventualTextViewUpdate(serviceFile));

		return promisedState;
	}

	private class EventualTextViewUpdate implements EventualAction {

		private final ServiceFile serviceFile;

		EventualTextViewUpdate(ServiceFile serviceFile) {
			this.serviceFile = serviceFile;
		}

		@Override
		public Promise<?> promiseAction() {
			synchronized (textViewUpdateSync) {
				currentlyPromisedTextViewUpdate = new PromisedTextViewUpdate(serviceFile);
				currentlyPromisedTextViewUpdate.beginUpdate();
				return currentlyPromisedTextViewUpdate;
			}
		}
	}

	private class PromisedTextViewUpdate extends Promise<Void> implements
		Runnable,
		ImmediateResponse<Map<String, String>, Void>,
		PromisedResponse<IConnectionProvider, Map<String, String>>
	{

		private final CancellationProxy cancellationProxy = new CancellationProxy();
		private final ServiceFile serviceFile;

		PromisedTextViewUpdate(ServiceFile serviceFile) {
			this.serviceFile = serviceFile;

			respondToCancellation(cancellationProxy);
		}

		void beginUpdate() {
			if (handler.getLooper().getThread() == Thread.currentThread()) {
				run();
				return;
			}

			if (handler.post(this)) return;

			logger.warn("Handler failed to post text view update: " + handler);
			resolve(null);
		}

		@Override
		public void run() {
			textView.setText(R.string.lbl_loading);

			final Promise<Void> promisedViewSetting =
				SessionConnection.getInstance(textView.getContext()).promiseSessionConnection()
					.eventually(this)
					.eventually(LoopedInPromise.response(this, handler));

			final Promise<Void> delayPromise = delay(timeoutDuration);
			cancellationProxy.doCancel(delayPromise);

			Promise.whenAny(promisedViewSetting, delayPromise)
				.must(() -> {
					// First, cancel everything if the delay promise finished first
					delayPromise.then(new VoidResponse<>($ -> cancellationProxy.run()));
					// Then cancel the delay promise, in case the promised view setting
					// finished first
					delayPromise.cancel();

					// Finally, always resolve the parent promise
					resolve(null);
				})
				.excuse(e -> {
					LoggerUncaughtExceptionHandler
						.getErrorExecutor()
						.execute(() -> handleError(e));

					return null;
				});
		}

		@Override
		public Promise<Map<String, String>> promiseResponse(IConnectionProvider connectionProvider) {
			if (isNotCurrentPromise() || isUpdateCancelled())
				return new Promise<>(Collections.emptyMap());

			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider =
				new CachedSessionFilePropertiesProvider(connectionProvider, filePropertyCache,
					new SessionFilePropertiesProvider(connectionProvider, filePropertyCache));

			final Promise<Map<String, String>> filePropertiesPromise = cachedSessionFilePropertiesProvider.promiseFileProperties(serviceFile);

			cancellationProxy.doCancel(filePropertiesPromise);

			return filePropertiesPromise;
		}

		@Override
		public Void respond(Map<String, String> properties) {
			if (isNotCurrentPromise() || isUpdateCancelled()) return null;

			final String fileName = properties.get(KnownFileProperties.NAME);

			if (fileName != null)
				textView.setText(fileName);

			return null;
		}

		private void handleError(Throwable e) {
			if (isUpdateCancelled()) return;

			if (e instanceof CancellationException) return;

			if (e instanceof SocketException) {
				final String message = e.getMessage();
				if (message != null && message.toLowerCase().contains("socket closed")) return;
			}

			if (e instanceof IOException) {
				final String message = e.getMessage();
				if (message != null && message.toLowerCase().contains("canceled")) return;
			}

			if (e instanceof SSLProtocolException) {
				final String message = e.getMessage();
				if (message != null && message.toLowerCase().contains("ssl handshake aborted")) return;
			}

			logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.getKey(), e);
		}

		private boolean isNotCurrentPromise() {
			synchronized (textViewUpdateSync) {
				return currentlyPromisedTextViewUpdate != this;
			}
		}

		private boolean isUpdateCancelled() {
			return cancellationProxy.isCancelled();
		}
	}
}
