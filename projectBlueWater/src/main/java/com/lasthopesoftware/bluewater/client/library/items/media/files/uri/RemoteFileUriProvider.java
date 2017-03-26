package com.lasthopesoftware.bluewater.client.library.items.media.files.uri;

import android.net.Uri;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 7/24/15.
 */
public class RemoteFileUriProvider implements IFileUriProvider {
	private final IConnectionProvider connectionProvider;

	public RemoteFileUriProvider(IConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public IPromise<Uri> getFileUri(ServiceFile serviceFile) {
		LoggerFactory.getLogger(RemoteFileUriProvider.class).info("Returning serviceFile URL from server.");

		/* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */

		final String itemUrl =
			connectionProvider
				.getUrlProvider()
				.getUrl(
					"ServiceFile/GetFile",
					"ServiceFile=" + Integer.toString(serviceFile.getKey()),
					"Quality=medium",
					"Conversion=Android",
					"Playback=0");

		return new Promise<>(Uri.parse(itemUrl));
	}
}
