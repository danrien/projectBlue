package com.lasthopesoftware.bluewater.client.connection.testing;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;

public class ConnectionTester implements TestConnections {

	private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

	@Override
	public Promise<Boolean> promiseIsConnectionPossible(IConnectionProvider connectionProvider) {
		return connectionProvider.promiseResponse("Alive").then(this::doTestSynchronously).then(forward(), e -> false);
	}

	private boolean doTestSynchronously(Response response) {
			try {
				try (final InputStream is = response.body().byteStream()) {
					final StandardRequest responseDao = StandardRequest.fromInputStream(is);

					return responseDao != null && responseDao.isStatus();
				} catch (IOException e) {
					mLogger.error("Error closing connection, device failure?", e);
				}
			} catch (IllegalArgumentException e) {
				mLogger.warn("Illegal argument passed in", e);
			}

		return false;
	}
}
