package com.lasthopesoftware.bluewater.client.playback.file.volume.specs.GivenAnInitializedMaxVolumeModifier;


import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager;
import com.lasthopesoftware.bluewater.client.playback.file.volume.PlaybackHandlerMaxVolumeModifier;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenSettingTheMaxVolume {
	private static NoTransformVolumeManager volumeManager;

	@BeforeClass
	public static void before() {
		volumeManager = new NoTransformVolumeManager();

		final PlaybackHandlerMaxVolumeModifier playbackHandlerMaxVolumeModifier = new PlaybackHandlerMaxVolumeModifier(volumeManager, .58f);
		playbackHandlerMaxVolumeModifier.setMaxFileVolume(.8f);
	}

	@Test
	public void thenThePlaybackHandlerVolumeIsCorrectlySet() {
		assertThat(volumeManager.getVolume()).isEqualTo(.464f);
	}
}
