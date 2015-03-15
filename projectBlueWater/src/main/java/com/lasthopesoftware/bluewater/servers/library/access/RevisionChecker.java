package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

import java.io.InputStream;
import java.net.HttpURLConnection;

public class RevisionChecker implements OnExecuteListener<Void, Void, Integer> {
	
	private final static Integer mBadRevision = Integer.valueOf(-1);
	private static Integer mCachedRevision = mBadRevision;

    private volatile long lastCheckedTime = -1;
    private final static long checkedExpirationTime = 30000;

	public static SimpleTask<Void, Void, Integer> getRevisionTask() {
		return new SimpleTask<Void, Void, Integer>(new RevisionChecker());
	}

	@Override
	public Integer onExecute(ISimpleTask<Void, Void, Integer> owner, Void... params) throws Exception {
        synchronized (mBadRevision) {
            if (!mCachedRevision.equals(mBadRevision) && System.currentTimeMillis() - checkedExpirationTime < lastCheckedTime) {
                return mCachedRevision;
            }

            final HttpURLConnection conn = ConnectionProvider.getConnection("Library/GetRevision");
            try {
                final InputStream is = conn.getInputStream();
                try {
                    final String revisionValue = StandardRequest.fromInputStream(is).items.get("Sync");

                    if (revisionValue == null || revisionValue.isEmpty()) return mBadRevision;

                    mCachedRevision = Integer.valueOf(revisionValue);
                    return mCachedRevision;
                } finally {
                    is.close();
                }
            } catch (Exception e) {
                return mBadRevision;
            } finally {
                conn.disconnect();
            }
        }
	}
}
