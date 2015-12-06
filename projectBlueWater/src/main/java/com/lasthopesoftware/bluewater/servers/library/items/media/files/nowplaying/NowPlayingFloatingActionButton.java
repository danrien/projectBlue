package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.runnables.ITwoParameterRunnable;
import com.lasthopesoftware.threading.IFluentTask;

/**
 * Created by david on 10/11/15.
 */
public class NowPlayingFloatingActionButton extends FloatingActionButton {
    private static Drawable nowPlayingIconDrawable;

    public static NowPlayingFloatingActionButton addNowPlayingFloatingActionButton(RelativeLayout container) {
        final NowPlayingFloatingActionButton nowPlayingFloatingActionButton = new NowPlayingFloatingActionButton(container.getContext());

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? RelativeLayout.ALIGN_PARENT_END : RelativeLayout.ALIGN_PARENT_RIGHT);
        final int margin = ViewUtils.dpToPx(container.getContext(), 16);
        layoutParams.setMargins(margin, margin, margin, margin);

        nowPlayingFloatingActionButton.setLayoutParams(layoutParams);
        container.addView(nowPlayingFloatingActionButton);

        return nowPlayingFloatingActionButton;
    }

    private boolean isNowPlayingFileSet;

    private NowPlayingFloatingActionButton(Context context) {
        super(context);

        if (nowPlayingIconDrawable == null)
            nowPlayingIconDrawable = context.getResources().getDrawable(R.drawable.av_play_dark);

        setImageDrawable(nowPlayingIconDrawable);

        initializeNowPlayingFloatingActionButton();
    }


    @SuppressWarnings("ResourceType")
    private void initializeNowPlayingFloatingActionButton() {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewUtils.CreateNowPlayingView(v.getContext());
            }
        });

        setVisibility(ViewUtils.getVisibility(false));
        // The user can change the library, so let's check if the state of visibility on the
        // now playing menu item should change
        LibrarySession.GetActiveLibrary(getContext(), new ITwoParameterRunnable<IFluentTask<Integer,Void,Library>, Library>() {

            @Override
            public void run(IFluentTask<Integer, Void, Library> owner, Library result) {
                isNowPlayingFileSet = result != null && result.getNowPlayingId() >= 0;
                setVisibility(ViewUtils.getVisibility(isNowPlayingFileSet));

                if (isNowPlayingFileSet) return;

                // If now playing shouldn't be visible, detect when it should be
                PlaybackService.addOnStreamingStartListener(new OnNowPlayingStartListener() {
                    @Override
                    public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
                        setVisibility(ViewUtils.getVisibility(true));
                        PlaybackService.removeOnStreamingStartListener(this);
                        isNowPlayingFileSet = true;
                    }
                });
            }
        });
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
