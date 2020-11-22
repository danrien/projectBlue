package com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.namehillsoftware.projectblue.shared.MagicPropertyBuilder;

public class PlaybackEngineTypeChangedBroadcaster {

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(PlaybackEngineTypeChangedBroadcaster.class);

	public static final String playbackEngineTypeChanged = magicPropertyBuilder.buildProperty("playbackEngineTypeChanged");
	public static final String playbackEngineTypeKey = magicPropertyBuilder.buildProperty("playbackEngineTypeKey");

	private final Context context;

	public PlaybackEngineTypeChangedBroadcaster(Context context) {
		this.context = context;
	}

	public void broadcastPlaybackEngineTypeChanged(PlaybackEngineType playbackEngineType) {
		final Intent intent = new Intent(playbackEngineTypeChanged);
		intent.putExtra(playbackEngineTypeKey, playbackEngineType.name());

		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(intent);
	}
}
