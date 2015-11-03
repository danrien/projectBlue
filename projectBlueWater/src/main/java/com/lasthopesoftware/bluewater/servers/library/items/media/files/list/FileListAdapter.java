package com.lasthopesoftware.bluewater.servers.library.items.media.files.list;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.disk.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers.IItemListMenuChangeHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.BaseMenuViewHolder;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.FilePlayClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.ViewFileDetailsClickListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.AbstractFileListItemNowPlayingHandler;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.FileListItemContainer;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.menu.GetFileListItemTextTask;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackController;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.servers.library.items.menu.LongClickViewAnimatorListener;
import com.lasthopesoftware.bluewater.servers.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.AbstractMenuClickHandler;
import com.lasthopesoftware.bluewater.servers.library.items.menu.handlers.ViewChangedHandler;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;

import java.util.List;

public class FileListAdapter extends AbstractFileListAdapter {

    private final ViewChangedHandler viewChangedHandler = new ViewChangedHandler();

    private static class ViewHolder extends BaseMenuViewHolder {

		public ViewHolder(final FileListItemContainer fileListItemContainer, final ImageButton viewFileDetailsButton, final ImageButton playButton, final ImageButton addButton) {
			super(viewFileDetailsButton, playButton);
			
			this.addButton = addButton;
            this.fileListItemContainer = fileListItemContainer;
		}

		public final ImageButton addButton;
        public final FileListItemContainer fileListItemContainer;
        public AbstractFileListItemNowPlayingHandler fileListItemNowPlayingHandler;
        public GetFileListItemTextTask getFileListItemTextTask;
	}

	public FileListAdapter(Context context, int resource, List<IFile> files, IItemListMenuChangeHandler itemListMenuChangeHandler) {
		super(context, resource, files);

        viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler);
        viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler);
        viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final IFile file = getItem(position);

        if (convertView == null) {
            final FileListItemContainer fileItemMenu = new FileListItemContainer(parent.getContext());
            final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator = fileItemMenu.getViewAnimator();
            convertView = notifyOnFlipViewAnimator;

            final LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_file_item_menu, parent, false);
            final ImageButton addButton = (ImageButton)fileMenu.findViewById(R.id.btnAddToPlaylist);
            final ImageButton playButton = (ImageButton)fileMenu.findViewById(R.id.btnPlaySong);
            final ImageButton viewFileDetailsButton = (ImageButton)fileMenu.findViewById(R.id.btnViewFileDetails);

            notifyOnFlipViewAnimator.addView(fileMenu);

            notifyOnFlipViewAnimator.setTag(new ViewHolder(fileItemMenu, viewFileDetailsButton, playButton, addButton));

            notifyOnFlipViewAnimator.setViewChangedListener(viewChangedHandler);
        }

        final ViewHolder viewHolder = (ViewHolder)convertView.getTag();

        final FileListItemContainer fileListItem = viewHolder.fileListItemContainer;

        final TextView textView = fileListItem.getTextView();

        if (viewHolder.getFileListItemTextTask != null) viewHolder.getFileListItemTextTask.cancel(false);
        viewHolder.getFileListItemTextTask = new GetFileListItemTextTask(file, textView);
        viewHolder.getFileListItemTextTask.execute();

        textView.setTypeface(null, Typeface.NORMAL);

        if (PlaybackService.getPlaylistController() != null && PlaybackService.getPlaylistController().getCurrentPlaybackFile() != null)
            textView.setTypeface(null, file.getKey() == PlaybackService.getPlaylistController().getCurrentPlaybackFile().getFile().getKey() ? Typeface.BOLD : Typeface.NORMAL);

        if (viewHolder.fileListItemNowPlayingHandler != null) viewHolder.fileListItemNowPlayingHandler.release();
        viewHolder.fileListItemNowPlayingHandler = new AbstractFileListItemNowPlayingHandler(fileListItem) {
            @Override
            public void onNowPlayingStart(PlaybackController controller, IPlaybackFile filePlayer) {
                textView.setTypeface(null, file.getKey() == filePlayer.getFile().getKey() ? Typeface.BOLD : Typeface.NORMAL);
            }
        };

        final NotifyOnFlipViewAnimator viewAnimator = fileListItem.getViewAnimator();
        LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator);
        viewHolder.playButton.setOnClickListener(new FilePlayClickListener(viewAnimator, position, getFiles()));
        viewHolder.viewFileDetailsButton.setOnClickListener(new ViewFileDetailsClickListener(viewAnimator, file));
        viewHolder.addButton.setOnClickListener(new AddClickListener(viewAnimator, file));

        return viewAnimator;
    }

	private static class AddClickListener extends AbstractMenuClickHandler {
		private IFile mFile;
		
		public AddClickListener(NotifyOnFlipViewAnimator viewFlipper, IFile file) {
            super(viewFlipper);
			mFile = file;
		}
		
		@Override
		public void onClick(final View view) {
			if (PlaybackService.getPlaylistController() != null) 
				PlaybackService.getPlaylistController().addFile(mFile);
			
			LibrarySession.GetActiveLibrary(view.getContext(), new OnCompleteListener<Integer, Void, Library>() {

                @Override
                public void onComplete(ISimpleTask<Integer, Void, Library> owner, Library result) {
                    if (result == null) return;
                    String newFileString = result.getSavedTracksString();
                    if (!newFileString.endsWith(";")) newFileString += ";";
                    newFileString += mFile.getKey() + ";";
                    result.setSavedTracksString(newFileString);

                    LibrarySession.SaveLibrary(view.getContext(), result, new OnCompleteListener<Void, Void, Library>() {

                        @Override
                        public void onComplete(ISimpleTask<Void, Void, Library> owner, Library result) {
                            Toast.makeText(view.getContext(), view.getContext().getText(R.string.lbl_song_added_to_now_playing), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            });

            super.onClick(view);
		}
	}

}