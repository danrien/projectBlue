package com.namehillsoftware.projectblue.playback.file.volume.preparation;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxFileVolumePreparer implements PlayableFilePreparationSource {

	private static final Logger logger = LoggerFactory.getLogger(MaxFileVolumePreparer.class);

	private final PlayableFilePreparationSource playableFilePreparationSource;
	private final ProvideMaxFileVolume provideMaxFileVolume;

	public MaxFileVolumePreparer(PlayableFilePreparationSource playableFilePreparationSource, ProvideMaxFileVolume provideMaxFileVolume) {
		this.playableFilePreparationSource = playableFilePreparationSource;
		this.provideMaxFileVolume = provideMaxFileVolume;
	}

	@Override
	public Promise<PreparedPlayableFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt) {
		final Promise<Float> promisedMaxFileVolume = provideMaxFileVolume.promiseMaxFileVolume(serviceFile);

		return playableFilePreparationSource
			.promisePreparedPlaybackFile(serviceFile, preparedAt)
			.then(ppf -> {
				final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(ppf.getPlayableFileVolumeManager());

				promisedMaxFileVolume
					.then(new VoidResponse<>(maxFileVolumeManager::setMaxFileVolume))
					.excuse(new VoidResponse<>(err -> logger.warn("There was an error getting the max file volume for file " + serviceFile, err)));

				return new PreparedPlayableFile(
					ppf.getPlaybackHandler(),
					maxFileVolumeManager,
					ppf.getBufferingPlaybackFile());
			});
	}
}
