package com.lasthopesoftware.bluewater.client.library.items.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.PlayClickHandler;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ShuffleClickHandler;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.SyncFilesIsVisibleHandler;
import com.lasthopesoftware.bluewater.client.library.items.menu.handlers.ViewFilesClickHandler;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;

public final class ListItemMenuBuilder<T extends IFileListParameterProvider & IItem> extends AbstractListItemMenuBuilder<T> {
	private static class ViewHolder {
		private final LazyViewFinder<TextView> textViewFinder;
		private final LazyViewFinder<ImageButton> shuffleButtonFinder;
		private final LazyViewFinder<ImageButton> playButtonFinder;
		private final LazyViewFinder<ImageButton> viewButtonFinder;
		private final LazyViewFinder<ImageButton> syncButtonFinder;


		public ViewHolder(
				LazyViewFinder<TextView> textViewFinder,
				LazyViewFinder<ImageButton> shuffleButtonFinder,
				LazyViewFinder<ImageButton> playButtonFinder,
				LazyViewFinder<ImageButton> viewButtonFinder,
				LazyViewFinder<ImageButton> syncButtonFinder) {
			this.textViewFinder = textViewFinder;
			this.shuffleButtonFinder = shuffleButtonFinder;
			this.playButtonFinder = playButtonFinder;
			this.viewButtonFinder = viewButtonFinder;
			this.syncButtonFinder = syncButtonFinder;
		}

		public View.OnLayoutChangeListener onSyncButtonLayoutChangeListener;

		public TextView getTextView() {
			return textViewFinder.findView();
		}

		public ImageButton getShuffleButton() {
			return shuffleButtonFinder.findView();
		}

		public ImageButton getPlayButton() {
			return playButtonFinder.findView();
		}

		public ImageButton getViewButton() {
			return viewButtonFinder.findView();
		}

		public ImageButton getSyncButton() {
			return syncButtonFinder.findView();
		}
	}

	@Override
	public View getView(int position, T item, View convertView, ViewGroup parent) {
		NotifyOnFlipViewAnimator parentView = (NotifyOnFlipViewAnimator)convertView;
		if (parentView == null) {
		
			final AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            parentView = new NotifyOnFlipViewAnimator(parent.getContext());
            parentView.setLayoutParams(lp);

			final LayoutInflater inflater = (LayoutInflater) parentView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			final LinearLayout listItemLayout = (LinearLayout) inflater.inflate(R.layout.layout_list_item, parentView, false);
			parentView.addView(listItemLayout);

			final LinearLayout fileMenu = (LinearLayout)inflater.inflate(R.layout.layout_browse_item_menu, parentView, false);
			parentView.addView(fileMenu);

			parentView.setTag(
				new ViewHolder(
					new LazyViewFinder<>(listItemLayout, R.id.tvListItem),
					new LazyViewFinder<>(fileMenu, R.id.btnShuffle),
					new LazyViewFinder<>(fileMenu, R.id.btnPlayAll),
					new LazyViewFinder<>(fileMenu, R.id.btnViewFiles),
					new LazyViewFinder<>(fileMenu, R.id.btnSyncItem)));
		}

		parentView.setViewChangedListener(getOnViewChangedListener());

		if (parentView.getDisplayedChild() != 0) parentView.showPrevious();
		
		final ViewHolder viewHolder = (ViewHolder) parentView.getTag();
		viewHolder.getTextView().setText(item.getValue());
		viewHolder.getShuffleButton().setOnClickListener(new ShuffleClickHandler(parentView, item));
		viewHolder.getPlayButton().setOnClickListener(new PlayClickHandler(parentView, item));
		viewHolder.getViewButton().setOnClickListener(new ViewFilesClickHandler(parentView, item));

		viewHolder.getSyncButton().setEnabled(false);

		if (viewHolder.onSyncButtonLayoutChangeListener != null)
			viewHolder.getSyncButton().removeOnLayoutChangeListener(viewHolder.onSyncButtonLayoutChangeListener);

		viewHolder.onSyncButtonLayoutChangeListener = new SyncFilesIsVisibleHandler(parentView, viewHolder.getSyncButton(), item);

		viewHolder.getSyncButton().addOnLayoutChangeListener(viewHolder.onSyncButtonLayoutChangeListener);

		return parentView;
	}
}
