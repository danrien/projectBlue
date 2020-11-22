package com.namehillsoftware.projectblue.playback.file.volume.preparation;

import com.namehillsoftware.projectblue.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;
import com.namehillsoftware.projectblue.playback.file.volume.ProvideMaxFileVolume;

public class MaxFileVolumePreparationProvider implements IPlayableFilePreparationSourceProvider {

	private final IPlayableFilePreparationSourceProvider preparationSourceProvider;
	private final ProvideMaxFileVolume maxFileVolume;

	public MaxFileVolumePreparationProvider(IPlayableFilePreparationSourceProvider preparationSourceProvider, ProvideMaxFileVolume maxFileVolume) {
		this.preparationSourceProvider = preparationSourceProvider;
		this.maxFileVolume = maxFileVolume;
	}

	@Override
	public PlayableFilePreparationSource providePlayableFilePreparationSource() {
		return new MaxFileVolumePreparer(preparationSourceProvider.providePlayableFilePreparationSource(), maxFileVolume);
	}

	@Override
	public int getMaxQueueSize() {
		return preparationSourceProvider.getMaxQueueSize();
	}
}
