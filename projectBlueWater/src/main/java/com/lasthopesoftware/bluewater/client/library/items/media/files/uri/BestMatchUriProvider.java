package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.content.Context;
import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.uri.StoredFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;

import java.io.IOException;

/**
 * Created by david on 7/24/15.
 * Will get the best URI for access speed.
 */
public class BestMatchUriProvider implements IFileUriProvider {
	private final Context context;
	private final Library library;
	private final IConnectionProvider connectionProvider;
	private final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator;
	private final StoredFileUriProvider storedFileUriProvider;
	private final MediaFileUriProvider mediaFileUriProvider;
	private final RemoteFileUriProvider remoteFileUriProvider;

	public BestMatchUriProvider(Context context, IConnectionProvider connectionProvider, Library library) {
		this(context, connectionProvider, library, new ExternalStorageReadPermissionsArbitratorForOs(context));
	}

	private BestMatchUriProvider(Context context, IConnectionProvider connectionProvider, Library library, IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator) {
		this.context = context;
		this.library = library;
		this.connectionProvider = connectionProvider;
		this.externalStorageReadPermissionsArbitrator = externalStorageReadPermissionsArbitrator;
		storedFileUriProvider = new StoredFileUriProvider(context, library, externalStorageReadPermissionsArbitrator);

		final IFilePropertiesContainerRepository filePropertiesContainerRepository = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertiesContainerRepository, new FilePropertiesProvider(connectionProvider, filePropertiesContainerRepository));
		mediaFileUriProvider = new MediaFileUriProvider(context, new MediaQueryCursorProvider(context, cachedFilePropertiesProvider), externalStorageReadPermissionsArbitrator, library);

		remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider);
	}

	@Override
	public IPromise<Uri> getFileUri(IFile file) throws IOException {
		return
			storedFileUriProvider
				.getFileUri(file)
				.thenPromise(storedFileUri -> {
					if (storedFileUri != null)
						return new Promise<>(storedFileUri);

					if (library.isUsingExistingFiles()) {
						return
							mediaFileUriProvider
								.getFileUri(file)
								.thenPromise(mediaFileUri -> {
									if (mediaFileUri != null)
										return new Promise<>(mediaFileUri);

									final RemoteFileUriProvider remoteFileUriProvider = new RemoteFileUriProvider(connectionProvider);
									return remoteFileUriProvider.getFileUri(file);
								});
					}

					return remoteFileUriProvider.getFileUri(file);
				});
	}
}
