package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.lasthopesoftware.bluewater.client.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.list.AbstractFileListAdapter;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.menu.NowPlayingFileListItemMenuBuilder;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ViewChangedHandler;
import com.vedsoft.futures.runnables.OneParameterRunnable;

import java.util.List;

public class NowPlayingFileListAdapter extends AbstractFileListAdapter implements OneParameterRunnable<Integer> {

    private final NowPlayingFileListItemMenuBuilder nowPlayingFileListItemMenuBuilder;

	public NowPlayingFileListAdapter(Context context, int resource, IItemListMenuChangeHandler itemListMenuChangeHandler, List<IFile> files, int nowPlayingFilePos) {
		super(context, resource, files);

        final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);

        nowPlayingFileListItemMenuBuilder = new NowPlayingFileListItemMenuBuilder(files, nowPlayingFilePos);
        nowPlayingFileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler);
        nowPlayingFileListItemMenuBuilder.setOnPlaylistFileRemovedListener(this);
	}

    @Override
    public final View getView(final int position, View convertView, final ViewGroup parent) {
        return nowPlayingFileListItemMenuBuilder.getView(position, getItem(position), convertView, parent);
    }

    @Override
    public void run(Integer position) {
        remove(getItem(position));
    }
}
