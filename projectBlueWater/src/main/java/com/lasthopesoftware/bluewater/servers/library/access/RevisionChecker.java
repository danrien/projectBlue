package com.lasthopesoftware.bluewater.servers.library.access;

import android.util.SparseIntArray;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.vedsoft.fluent.FluentTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevisionChecker extends FluentTask<Void, Void, Integer> {
	
	private final static Integer mBadRevision = -1;
    private static final SparseIntArray cachedRevisions = new SparseIntArray();

    private static long mLastCheckedTime = -1;
    private final static long mCheckedExpirationTime = 30000;

    private static final ExecutorService revisionExecutor = Executors.newSingleThreadExecutor();

	private final ConnectionProvider connectionProvider;

	public static Integer getRevision(ConnectionProvider connectionProvider) {
        try {
            return (new RevisionChecker(connectionProvider)).get(revisionExecutor);
        } catch (ExecutionException | InterruptedException e) {
            return getCachedRevision(connectionProvider);
        }
    }

    private static Integer getCachedRevision(ConnectionProvider connectionProvider) {
        final int libraryId = connectionProvider.getAccessConfiguration().getLibraryId();
        if (cachedRevisions.indexOfKey(libraryId) < 0)
            cachedRevisions.put(libraryId, mBadRevision);

        return cachedRevisions.get(libraryId);
    }

    private RevisionChecker(ConnectionProvider connectionProvider) {
	    this.connectionProvider = connectionProvider;
    }

    @Override
    protected Integer executeInBackground(Void... params) {
        if (!getCachedRevision(connectionProvider).equals(mBadRevision) && System.currentTimeMillis() - mCheckedExpirationTime < mLastCheckedTime) {
            return getCachedRevision(connectionProvider);
        }

        try {
            final HttpURLConnection conn = connectionProvider.getConnection("Library/GetRevision");
            try {
                final InputStream is = conn.getInputStream();
                try {
                    final StandardRequest standardRequest = StandardRequest.fromInputStream(is);
                    if (standardRequest == null)
                        return getCachedRevision(connectionProvider);

                    final String revisionValue = standardRequest.items.get("Sync");

                    if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

                    cachedRevisions.put(connectionProvider.getAccessConfiguration().getLibraryId(), Integer.valueOf(revisionValue));
                    mLastCheckedTime = System.currentTimeMillis();
                    return getCachedRevision(connectionProvider);
                } finally {
                    is.close();
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return getCachedRevision(connectionProvider);
        }
    }
}
