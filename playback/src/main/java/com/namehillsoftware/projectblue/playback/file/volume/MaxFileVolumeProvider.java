package com.namehillsoftware.projectblue.playback.file.volume;

import com.namehillsoftware.handoff.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxFileVolumeProvider implements ProvideMaxFileVolume {

	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumeProvider.class);

	private static final float MaxRelativeVolumeInDecibels = 23;

	private static final float MaxAbsoluteVolumeInDecibels = 89;

	private static final float MaxComputedVolumeInDecibels = MaxAbsoluteVolumeInDecibels + MaxRelativeVolumeInDecibels;

	private static final float UnityVolume = 1.0f;

	private static final Promise<Float> promisedUnityVolume = new Promise<>(UnityVolume);

	private final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider;
	private final IVolumeLevelSettings volumeLevelSettings;

	public MaxFileVolumeProvider(IVolumeLevelSettings volumeLevelSettings, CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider) {
		this.volumeLevelSettings = volumeLevelSettings;
		this.cachedSessionFilePropertiesProvider = cachedSessionFilePropertiesProvider;
	}

	@Override
	public Promise<Float> promiseMaxFileVolume(ServiceFile serviceFile) {
		if (!volumeLevelSettings.isVolumeLevellingEnabled())
			return promisedUnityVolume;

		return
			cachedSessionFilePropertiesProvider
				.promiseFileProperties(serviceFile)
				.then(fileProperties -> {
					if (!fileProperties.containsKey(KnownFileProperties.VolumeLevelR128))
						return UnityVolume;

					final String r128VolumeLevelString = fileProperties.get(KnownFileProperties.VolumeLevelR128);
					try {
						final float r128VolumeLevel = Float.parseFloat(r128VolumeLevelString);

						final float normalizedVolumeLevel = MaxRelativeVolumeInDecibels - r128VolumeLevel;

						return Math.min(1 - (normalizedVolumeLevel / MaxComputedVolumeInDecibels), UnityVolume);
					} catch (NumberFormatException e) {
						logger.info("There was an error attempting to parse the given R128 level of " + r128VolumeLevelString + ".", e);
					}

					return UnityVolume;
				});
	}
}
