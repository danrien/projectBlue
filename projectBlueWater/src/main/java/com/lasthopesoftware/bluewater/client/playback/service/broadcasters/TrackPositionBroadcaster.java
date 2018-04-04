package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFileProgress;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import io.reactivex.functions.Consumer;

public class TrackPositionBroadcaster implements Consumer<PlayingFileProgress> {
	private CreateAndHold<LocalBroadcastManager> lazyLocalBroadcastManager;

	public TrackPositionBroadcaster(Context context) {
		lazyLocalBroadcastManager = new Lazy<>(() -> LocalBroadcastManager.getInstance(context));
	}

	@Override
	public void accept(PlayingFileProgress playingFileProgress) {
		final Intent trackPositionChangedIntent = new Intent(trackPositionUpdate);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.filePosition, playingFileProgress.position);
		trackPositionChangedIntent.putExtra(TrackPositionChangedParameters.fileDuration, playingFileProgress.duration);

		lazyLocalBroadcastManager.getObject().sendBroadcast(trackPositionChangedIntent);
	}

	private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionBroadcaster.class);

	public static final String trackPositionUpdate = magicPropertyBuilder.buildProperty("trackPositionUpdate");

	public static class TrackPositionChangedParameters {
		private static final MagicPropertyBuilder magicPropertyBuilder = new MagicPropertyBuilder(TrackPositionChangedParameters.class);

		public static final String filePosition = magicPropertyBuilder.buildProperty("filePosition");
		public static final String fileDuration = magicPropertyBuilder.buildProperty("fileDuration");
	}
}
