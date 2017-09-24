package com.lasthopesoftware.bluewater.client.library.items.stored.specs;

import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;

import java.util.Collection;

public abstract class FakeDeferredStoredItemAccess implements IStoredItemAccess {

	private Messenger<Collection<StoredItem>> messenger;

	public void resolveStoredItems() {
		if (messenger != null) {
			messenger.sendResolution(getStoredItems());
		}
	}

	protected abstract Collection<StoredItem> getStoredItems();

	@Override
	public void toggleSync(IItem item, boolean enable) {

	}

	@Override
	public Promise<Boolean> isItemMarkedForSync(IItem item) {
		return new Promise<>(false);
	}

	@Override
	public Promise<Collection<StoredItem>> promiseStoredItems() {
		return new Promise<>((m -> messenger = m));
	}
}
