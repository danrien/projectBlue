package com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.fileproperties;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.IFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.playstats.IPlaystatsUpdate;
import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilePropertiesPlayStatsUpdater implements IPlaystatsUpdate {
	private static final Logger logger = LoggerFactory.getLogger(FilePropertiesPlayStatsUpdater.class);

	private final IFilePropertiesProvider filePropertiesProvider;
	private final FilePropertiesStorage filePropertiesStorage;

	public FilePropertiesPlayStatsUpdater(IFilePropertiesProvider filePropertiesProvider, FilePropertiesStorage filePropertiesStorage) {
		this.filePropertiesProvider = filePropertiesProvider;
		this.filePropertiesStorage = filePropertiesStorage;
	}

	@Override
	public Promise<?> promisePlaystatsUpdate(ServiceFile serviceFile) {
		return filePropertiesProvider.promiseFileProperties(serviceFile)
			.eventually(fileProperties -> {
				try {
					final String lastPlayedServer = fileProperties.get(FilePropertiesProvider.LAST_PLAYED);
					final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

					final long currentTime = System.currentTimeMillis();
					if (lastPlayedServer != null && (currentTime - duration) <= Long.valueOf(lastPlayedServer) * 1000)
						return Promise.empty();

					final String numberPlaysString = fileProperties.get(FilePropertiesProvider.NUMBER_PLAYS);

					int numberPlays = 0;
					if (numberPlaysString != null && !numberPlaysString.isEmpty())
						numberPlays = Integer.parseInt(numberPlaysString);

					final Promise<Void> numberPlaysUpdate = filePropertiesStorage.promiseFileUpdate(serviceFile, FilePropertiesProvider.NUMBER_PLAYS, String.valueOf(++numberPlays), false);

					final String newLastPlayed = String.valueOf(currentTime / 1000);
					final Promise<Void> lastPlayedUpdate = filePropertiesStorage.promiseFileUpdate(serviceFile, FilePropertiesProvider.LAST_PLAYED, newLastPlayed, false);

					return Promise.whenAll(numberPlaysUpdate, lastPlayedUpdate);
				} catch (NumberFormatException ne) {
					logger.error(ne.toString(), ne);
				}

				return Promise.empty();
			});
	}
}
