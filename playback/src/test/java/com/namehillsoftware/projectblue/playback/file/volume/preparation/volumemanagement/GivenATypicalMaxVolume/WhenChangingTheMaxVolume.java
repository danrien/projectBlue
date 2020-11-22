package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.volumemanagement.GivenATypicalMaxVolume;


import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumeManager;

import org.junit.BeforeClass;
import org.junit.Test;

public class WhenChangingTheMaxVolume {
	private static NoTransformVolumeManager volumeManager;

	@BeforeClass
	public static void before() {
		volumeManager = new NoTransformVolumeManager();

		final MaxFileVolumeManager maxFileVolumeManager = new MaxFileVolumeManager(volumeManager);
		maxFileVolumeManager.setVolume(.58f);
		maxFileVolumeManager.setMaxFileVolume(.8f);
		maxFileVolumeManager.setMaxFileVolume(.47f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(volumeManager.getVolume()).isEqualTo(.2726f);
	}
}
