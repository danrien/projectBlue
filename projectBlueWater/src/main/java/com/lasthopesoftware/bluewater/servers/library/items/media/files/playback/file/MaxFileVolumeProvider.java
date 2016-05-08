package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.content.Context;
import android.preference.PreferenceManager;

import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.properties.FilePropertiesProvider;
import com.vedsoft.fluent.FluentTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by david on 5/8/16.
 */
public class MaxFileVolumeProvider extends FluentTask<Void, Void, Float> {

	private static Logger logger = LoggerFactory.getLogger(MaxFileVolumeProvider.class);

	private static final float MaxRelativeVolumeInDecibels = 23;

	private static final float MaxAbsoluteVolumeInDecibels = 89;

	private static final float MaxComputedVolumeInDecibels = MaxAbsoluteVolumeInDecibels + MaxRelativeVolumeInDecibels;

	private static final float UnityVolume = 1.0f;

	private final Context context;

	private final ConnectionProvider connectionProvider;

	private final IFile file;

	public MaxFileVolumeProvider(Context context, ConnectionProvider connectionProvider, IFile file) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.file = file;
	}

	@Override
	protected Float executeInBackground(Void[] params) {
		final boolean isVolumeLevelingEnabled =
				PreferenceManager
						.getDefaultSharedPreferences(context)
						.getBoolean(ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, false);

		if (!isVolumeLevelingEnabled)
			return UnityVolume;

		try {
			final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(this.connectionProvider, file.getKey());
			filePropertiesProvider.execute();

			final Map<String, String> fileProperties = filePropertiesProvider.get();
			if (!fileProperties.containsKey(FilePropertiesProvider.VolumeLevelR128))
				return UnityVolume;

			final float r128VolumeLevel = Float.parseFloat(fileProperties.get(FilePropertiesProvider.VolumeLevelR128));

			return Math.min(1 - (r128VolumeLevel / MaxComputedVolumeInDecibels), UnityVolume);
		} catch (ExecutionException | InterruptedException e) {
			logger.warn("There was an error getting the max file volume", e);
		}

		return UnityVolume;
	}
}
