package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.GivenABasePreparationSourceProvider;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenGettingTheMaxQueueSize {

	private static int maxQueueSize;

	@BeforeClass
	public static void setup() {
		final MaxFileVolumePreparationProvider maxFileVolumePreparationProvider = new MaxFileVolumePreparationProvider(new IPlayableFilePreparationSourceProvider() {
			@Override
			public PlayableFilePreparationSource providePlayableFilePreparationSource() {
				return null;
			}

			@Override
			public int getMaxQueueSize() {
				return 13;
			}
		}, mock(ProvideMaxFileVolume.class));

		maxQueueSize = maxFileVolumePreparationProvider.getMaxQueueSize();
	}

	@Test
	public void thenTheQueueSizeIsTheBaseQueueSize() {
		assertThat(maxQueueSize).isEqualTo(13);
	}
}
