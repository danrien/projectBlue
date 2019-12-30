package com.lasthopesoftware.bluewater.client.library.items.menu.handlers;

import android.view.View;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.menu.NotifyOnFlipViewAnimator;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.service.StoredSyncService;

/**
 * Created by david on 7/18/15.
 */
public class SyncFilesClickHandler extends  AbstractMenuClickHandler {
	private final StoredItemAccess syncListManager;
	private boolean isSynced;
	private final Library library;
	private final IItem item;

	SyncFilesClickHandler(NotifyOnFlipViewAnimator menuContainer, Library library, IItem item, boolean isSynced) {
		super(menuContainer);
		this.library = library;
		this.item = item;
		this.isSynced = isSynced;
		syncListManager = new StoredItemAccess(menuContainer.getContext());
	}

	@Override
	public void onClick(View v) {
		isSynced = !isSynced;
		syncListManager.toggleSync(library.getLibraryId(), item, isSynced);

		StoredSyncService.doSyncUninterrupted(v.getContext());

		super.onClick(v);
	}
}
