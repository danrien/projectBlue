package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertyHelpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 3/5/16.
 */
public class UpdatePlayStatsOnExecute implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(UpdatePlayStatsOnExecute.class);

	private final IConnectionProvider connectionProvider;
	private final IFile file;

	public UpdatePlayStatsOnExecute(IConnectionProvider connectionProvider, IFile file) {
		this.connectionProvider = connectionProvider;
		this.file = file;
	}

	@Override
	public void run() {
		try {
			final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(connectionProvider, file.getKey());
			final Map<String, String> fileProperties = filePropertiesProvider.get();
			final String lastPlayedServer = fileProperties.get(FilePropertiesProvider.LAST_PLAYED);
			final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

			if (lastPlayedServer != null && (System.currentTimeMillis() - duration) <= Long.valueOf(lastPlayedServer) * 1000) return;

			final String numberPlaysString = fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS);

			int numberPlays = 0;
			if (numberPlaysString != null && !numberPlaysString.isEmpty())
				numberPlays = Integer.parseInt(numberPlaysString);

			FilePropertiesStorage.storeFileProperty(connectionProvider, file.getKey(), FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays));

			final String lastPlayed = String.valueOf(System.currentTimeMillis() / 1000);
			FilePropertiesStorage.storeFileProperty(connectionProvider, file.getKey(), FilePropertiesProvider.LAST_PLAYED, lastPlayed);
		} catch (InterruptedException | ExecutionException e) {
			logger.warn(e.toString(), e);
		} catch (NumberFormatException ne) {
			logger.error(ne.toString(), ne);
		}
	}
}
