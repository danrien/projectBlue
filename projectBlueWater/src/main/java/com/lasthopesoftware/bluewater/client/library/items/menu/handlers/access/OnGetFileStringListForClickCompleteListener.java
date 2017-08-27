package com.lasthopesoftware.bluewater.client.library.items.menu.handlers.access;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.messenger.promises.response.ImmediateResponse;

public final class OnGetFileStringListForClickCompleteListener implements ImmediateResponse<String, Void> {
    private final Context mContext;

    public OnGetFileStringListForClickCompleteListener(final Context context) {
        mContext = context;
    }

    @Override
    public Void respond(String result) throws Throwable {
        PlaybackService.launchMusicService(mContext, result);
        return null;
    }
}
