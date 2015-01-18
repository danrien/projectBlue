package com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.receivers;

import com.lasthopesoftware.bluewater.servers.library.items.files.nowplaying.service.NowPlayingService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MusicIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))
			NowPlayingService.pause(context);
	}
}
