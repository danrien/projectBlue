package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.RevisionChecker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.providers.AbstractProvider;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;

public class FilePropertiesStorage implements Runnable {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FilePropertiesStorage.class);
	private final IConnectionProvider connectionProvider;
	private final int fileKey;
	private final String property;
	private final String value;
	private final boolean isFormatted;

	public static void storeFileProperty(IConnectionProvider connectionProvider, int fileKey, String property, String value, boolean isFormatted) {
		AbstractProvider.providerExecutor.execute(new FilePropertiesStorage(connectionProvider, fileKey, property, value, isFormatted));
	}

	private FilePropertiesStorage(IConnectionProvider connectionProvider, int fileKey, String property, String value, boolean isFormatted) {
		this.connectionProvider = connectionProvider;
		this.fileKey = fileKey;
		this.property = property;
		this.value = value;
		this.isFormatted = isFormatted;
	}

	@Override
	public void run() {
//		if (cancellation.isCancelled()) return null;

		try {
			final HttpURLConnection connection = connectionProvider.getConnection(
				"File/SetInfo",
				"File=" + String.valueOf(fileKey),
				"Field=" + property,
				"Value=" + value,
				"formatted=" + (isFormatted ? "1" : "0"));
			try {
				final int responseCode = connection.getResponseCode();
				logger.info("api/v1/File/SetInfo responded with a response code of " + responseCode);
			} finally {
				connection.disconnect();
			}
		} catch (IOException ioe) {
			logger.error("There was an error opening the connection", ioe);
		}

		RevisionChecker.promiseRevision(connectionProvider)
			.then(revision -> {
				final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), fileKey);
				final FilePropertiesContainer filePropertiesContainer = FilePropertyCache.getInstance().getFilePropertiesContainer(urlKeyHolder);

				if (filePropertiesContainer.revision == revision) filePropertiesContainer.updateProperty(property, value);

				return null;
			})
			.excuse(e -> {
				logger.warn(fileKey + "'s property cache item " + property + " was not updated with the new value of " + value, e);
				return null;
			});
	}
}
