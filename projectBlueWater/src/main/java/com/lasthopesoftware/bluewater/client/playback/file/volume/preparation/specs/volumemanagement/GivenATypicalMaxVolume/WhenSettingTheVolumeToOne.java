package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheVolumeToOne {

	private static NoTransformVolumeManager volumeManager;
	private static float returnedVolume;

	@BeforeClass
	public static void before() {
		volumeManager = new NoTransformVolumeManager();

		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager);
		maxFileVolumeManager.setMaxFileVolume(.8f);
		returnedVolume = maxFileVolumeManager.setVolume(1f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsSetToTheMaxVolume() {
		assertThat(volumeManager.getVolume()).isEqualTo(.8f);
	}

	@Test
	public void thenTheReturnedVolumeIsSetToTheMaxVolume() {
		assertThat(returnedVolume).isEqualTo(.8f);
	}
}
