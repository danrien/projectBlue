package com.namehillsoftware.projectblue.playback.volume;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;

public class AudioTrackVolumeManager
implements
	IVolumeManagement,
	ManagePlayableFileVolume {

	private final ExoPlayer exoPlayer;
	private final MediaCodecAudioRenderer[] audioRenderers;
	private float volume;

	public AudioTrackVolumeManager(ExoPlayer exoPlayer, MediaCodecAudioRenderer[] audioRenderers) {
		this.exoPlayer = exoPlayer;
		this.audioRenderers = audioRenderers;
	}

	@Override
	public float setVolume(float volume) {
		this.volume = volume;

		for (MediaCodecAudioRenderer renderer : audioRenderers) {
			exoPlayer.createMessage(renderer)
				.setType(C.MSG_SET_VOLUME)
				.setPayload(this.volume)
				.send();
		}

		return this.volume;
	}

	@Override
	public float getVolume() {
		return volume;
	}
}
