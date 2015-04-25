package com.lasthopesoftware.bluewater.servers.library.items.media.files.menu;

import android.view.View;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;

/**
 * Created by david on 4/14/15.
 */
public abstract class AbstractFileListItemNowPlayingHandler implements OnNowPlayingStartListener {

    private final RelativeLayout mFileTextViewContainer;

    private View.OnAttachStateChangeListener onAttachStateChangeListener;

    public AbstractFileListItemNowPlayingHandler(FileListItemContainer fileListItem) {
        mFileTextViewContainer = fileListItem.getTextViewContainer();
        if (mFileTextViewContainer == null)
            throw new IllegalArgumentException("fileListItem.getTextView() cannot be null");

        final OnNowPlayingStartListener onNowPlayingStartListener = this;

        PlaybackService.addOnStreamingStartListener(onNowPlayingStartListener);

        onAttachStateChangeListener = new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View v) {
                PlaybackService.removeOnStreamingStartListener(onNowPlayingStartListener);
            }

            @Override
            public void onViewAttachedToWindow(View v) { }
        };

        mFileTextViewContainer.addOnAttachStateChangeListener(onAttachStateChangeListener);
    }

    public void release() {
        PlaybackService.removeOnStreamingStartListener(this);
        mFileTextViewContainer.removeOnAttachStateChangeListener(onAttachStateChangeListener);
    }
}
