package com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.specs.GivenABasePreparationSourceProvider;

import com.lasthopesoftware.bluewater.client.playback.engine.preparation.IPlayableFilePreparationSourceProvider;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ProvideMaxFileVolume;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparationProvider;
import com.lasthopesoftware.bluewater.client.playback.file.volume.preparation.MaxFileVolumePreparer;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.projectblue.playback.file.preparation.PlayableFilePreparationSource;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenGettingAPreparationSource {
	private static PlayableFilePreparationSource playableFileSource;

	@BeforeClass
	public static void setup() {
		final MaxFileVolumePreparationProvider maxFileVolumePreparationProvider = new MaxFileVolumePreparationProvider(new IPlayableFilePreparationSourceProvider() {
			@Override
			public PlayableFilePreparationSource providePlayableFilePreparationSource() {
				return (sf, startAt) -> new Promise<>(new PreparedPlayableFile(
					new EmptyPlaybackHandler(0),
					mock(ManagePlayableFileVolume.class),
					new IBufferingPlaybackFile() {
						@Override
						public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
							return new Promise<>(this);
						}
					}
				));
			}

			@Override
			public int getMaxQueueSize() {
				return 13;
			}
		}, mock(ProvideMaxFileVolume.class));

		playableFileSource = maxFileVolumePreparationProvider.providePlayableFilePreparationSource();
	}

	@Test
	public void thenThePlayableFileSourceIsAMaxVolumePreparer() {
		assertThat(playableFileSource).isInstanceOf(MaxFileVolumePreparer.class);
	}

}
