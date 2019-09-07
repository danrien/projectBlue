package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

public class SyncFilesIsVisibleHandler implements View.OnLayoutChangeListener {

	private final NotifyOnFlipViewAnimator notifyOnFlipViewAnimator;
	private final ImageButton syncButton;
	private final StoredItemAccess storedItemAccess;
	private final Library library;
	private final IItem item;

	public SyncFilesIsVisibleHandler(NotifyOnFlipViewAnimator notifyOnFlipViewAnimator, ImageButton syncButton, StoredItemAccess storedItemAccess, Library library, IItem item) {
		this.notifyOnFlipViewAnimator = notifyOnFlipViewAnimator;
		this.syncButton = syncButton;
		this.storedItemAccess = storedItemAccess;
		this.library = library;
		this.item = item;
	}

	@Override
	public void onLayoutChange(final View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if (!v.isShown()) return;

		storedItemAccess
			.isItemMarkedForSync(item)
			.eventually(LoopedInPromise.response(isSynced -> {
				if (!v.isShown()) return null;

				syncButton.setImageDrawable(ContextCompat.getDrawable(v.getContext(), isSynced ? R.drawable.ic_sync_on : R.drawable.ic_sync_off));
				syncButton.setOnClickListener(new SyncFilesClickHandler(notifyOnFlipViewAnimator, library, item, isSynced));
				syncButton.setEnabled(true);

				return null;
			}, v.getContext()));
	}
}
