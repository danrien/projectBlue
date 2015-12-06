package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.threading.IFluentTask;

/**
 * Created by david on 4/3/15.
 */
public class OnGetFileStringListForClickCompleteListener implements IFluentTask.OnCompleteListener<Void, Void, String> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public void onComplete(IFluentTask<Void, Void, String> owner, String result) {
        PlaybackService.launchMusicService(mContext, result);
    }
}
