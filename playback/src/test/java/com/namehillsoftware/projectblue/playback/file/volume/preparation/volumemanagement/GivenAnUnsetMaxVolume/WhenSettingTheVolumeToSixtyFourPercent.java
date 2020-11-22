package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenAnUnsetMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class WhenSettingTheVolumeToSixtyFourPercent {

	private static final NoTransformVolumeManager volumeManager = new NoTransformVolumeManager();
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager);
		returnedVolume = maxFileVolumeManager.setVolume(.64f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheCorrectVolume() {
		assertThat(volumeManager.getVolume()).isCloseTo(.64f, offset(.00001f));
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheCorrectVolume() {
		assertThat(returnedVolume).isCloseTo(.64f, offset(.00001f));
	}
}
