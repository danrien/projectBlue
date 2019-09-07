package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity.NowPlayingActivity;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

public class NowPlayingFloatingActionButton extends FloatingActionButton {
    public static NowPlayingFloatingActionButton addNowPlayingFloatingActionButton(RelativeLayout container) {
        final NowPlayingFloatingActionButton nowPlayingFloatingActionButton = new NowPlayingFloatingActionButton(container.getContext());

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        final int margin = ViewUtils.dpToPx(container.getContext(), 16);
        layoutParams.setMargins(margin, margin, margin, margin);

        nowPlayingFloatingActionButton.setLayoutParams(layoutParams);
        container.addView(nowPlayingFloatingActionButton);

        return nowPlayingFloatingActionButton;
    }

    private boolean isNowPlayingFileSet;

    private NowPlayingFloatingActionButton(Context context) {
        super(context);

        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.av_play_dark));

        initializeNowPlayingFloatingActionButton();
    }


    @SuppressWarnings("ResourceType")
    private void initializeNowPlayingFloatingActionButton() {
        setOnClickListener(v -> NowPlayingActivity.startNowPlayingActivity(v.getContext()));

        setVisibility(ViewUtils.getVisibility(false));
        // The user can change the library, so let's check if the state of visibility on the
        // now playing menu item should change
        NowPlayingFileProvider
            .fromActiveLibrary(getContext())
            .getNowPlayingFile()
            .then(new VoidResponse<>(result -> {
                isNowPlayingFileSet = result != null;
                setVisibility(ViewUtils.getVisibility(isNowPlayingFileSet));

                if (isNowPlayingFileSet) return;

                final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());

                localBroadcastManager.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public synchronized void onReceive(Context context, Intent intent) {
                        isNowPlayingFileSet = true;
                        setVisibility(ViewUtils.getVisibility(true));
                        localBroadcastManager.unregisterReceiver(this);
                    }
                }, new IntentFilter(PlaylistEvents.onPlaylistStart));
            }));
    }

    @Override
    public void show() {
        if (isNowPlayingFileSet) super.show();
    }

    @Override
    public void show(OnVisibilityChangedListener listener) {
        if (isNowPlayingFileSet) super.show(listener);
    }
}
