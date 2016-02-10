package com.lasthopesoftware.bluewater.servers.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListProvider;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.access.OnGetFileStringListForClickErrorListener;

/**
 * Created by david on 4/3/15.
 */
public final class PlayClickHandler extends AbstractMenuClickHandler {
    private final IFileListParameterProvider item;

    public PlayClickHandler(NotifyOnFlipViewAnimator menuContainer, IFileListParameterProvider item) {
        super(menuContainer);

	    this.item = item;
    }

    @Override
    public void onClick(final View v) {
	    (new FileStringListProvider(SessionConnection.getSessionConnectionProvider(), item))
			    .onComplete(new OnGetFileStringListForClickCompleteListener(v.getContext()))
			    .onError(new OnGetFileStringListForClickErrorListener(v, this))
	            .execute();

        super.onClick(v);
    }
}