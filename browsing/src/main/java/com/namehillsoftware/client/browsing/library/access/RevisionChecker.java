package com.namehillsoftware.client.browsing.library.access;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;

public class RevisionChecker {

	private static final Integer badRevision = -1;
    private static final long checkedExpirationTime = Duration.standardSeconds(30).getMillis();

	private static final Map<String, Integer> cachedRevisions = new HashMap<>();
	private static final Map<String, Long> lastRevisions = new HashMap<>();

	private final IConnectionProvider connectionProvider;

	public static Promise<Integer> promiseRevision(IConnectionProvider connectionProvider) {
		return new RevisionChecker(connectionProvider).getRevision();
	}

    private static Integer getCachedRevision(IConnectionProvider connectionProvider) {
        final String serverUrl = connectionProvider.getUrlProvider().getBaseUrl();
        if (!cachedRevisions.containsKey(serverUrl))
            cachedRevisions.put(serverUrl, badRevision);

        return cachedRevisions.get(serverUrl);
    }

    private RevisionChecker(IConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

	private Promise<Integer> getRevision() {
		final String baseServerUrl = connectionProvider.getUrlProvider().getBaseUrl();
		final Long lastRevisionCheckedTime = lastRevisions.get(baseServerUrl);
        if (lastRevisionCheckedTime != null && !getCachedRevision(connectionProvider).equals(badRevision) && System.currentTimeMillis() - checkedExpirationTime < lastRevisionCheckedTime) {
            return new Promise<>(getCachedRevision(connectionProvider));
        }

        return connectionProvider.promiseResponse("Library/GetRevision")
			.then(response -> {
				final ResponseBody body = response.body();
				if (body == null) return getCachedRevision(connectionProvider);

				try (final InputStream is = body.byteStream()) {
					final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
					if (standardRequest == null)
						return getCachedRevision(connectionProvider);

					final String revisionValue = standardRequest.items.get("Sync");

					if (revisionValue == null || revisionValue.isEmpty()) return badRevision;

					cachedRevisions.put(baseServerUrl, Integer.valueOf(revisionValue));
					lastRevisions.put(baseServerUrl, System.currentTimeMillis());
					return getCachedRevision(connectionProvider);
				} finally {
					body.close();
				}
			}, e -> getCachedRevision(connectionProvider));
    }
}
