package com.lasthopesoftware.bluewater.client.sync.library.items;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface CheckIfAnyStoredItemsExist {
	Promise<Boolean> promiseIsAnyStoredItemsOrFiles(Library library);
}
