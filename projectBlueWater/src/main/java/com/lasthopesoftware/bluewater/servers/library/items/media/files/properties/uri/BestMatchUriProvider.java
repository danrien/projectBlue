package com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.store.Library;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider extends AbstractFileUriProvider {
	private final Context context;
	private final Library library;
	private final ConnectionProvider connectionProvider;

	public BestMatchUriProvider(Context context, ConnectionProvider connectionProvider, Library library, IFile file) {
		super(file);

		this.context = context;
		this.library = library;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Uri getFileUri() throws IOException {
		final StoredFileUriProvider storedFileUriProvider = new StoredFileUriProvider(context, library, getFile());
		Uri fileUri = storedFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		final MediaFileUriProvider mediaFileUriProvider = new MediaFileUriProvider(context, getFile());
		fileUri = mediaFileUriProvider.getFileUri();
		if (fileUri != null)
			return fileUri;

		final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider, getFile());
		return remoteFileUriProvider.getFileUri();
	}
}
