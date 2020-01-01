package com.lasthopesoftware.bluewater.client.library.items.media.files.properties;

import java.util.Map;

/**
 * Created by david on 3/5/16.
 */
public class FilePropertyHelpers {
	/*
	 * Get the duration of the serviceFile in milliseconds
	 */
	public static int parseDurationIntoMilliseconds(Map<String, String> fileProperties) {
		String durationToParse = fileProperties.get(SessionFilePropertiesProvider.DURATION);
		if (durationToParse != null && !durationToParse.isEmpty())
			return (int) (Double.parseDouble(durationToParse) * 1000);

		return -1;
	}
}
