package com.lasthopesoftware.bluewater.client.library.items.stored;

import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.CheckForAnyStoredFiles;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public class StoredItemsChecker implements CheckIfAnyStoredItemsExist {
	private final IStoredItemAccess storedItemAccess;
	private final CheckForAnyStoredFiles checkForAnyStoredFiles;

	public StoredItemsChecker(IStoredItemAccess storedItemAccess, CheckForAnyStoredFiles checkForAnyStoredFiles) {
		this.storedItemAccess = storedItemAccess;
		this.checkForAnyStoredFiles = checkForAnyStoredFiles;
	}

	@Override
	public Promise<Boolean> promiseIsAnyStoredItemsOrFiles(Library library) {
		return storedItemAccess.promiseStoredItems()
			.eventually(items -> !items.isEmpty()
				? new Promise<>(true)
				: checkForAnyStoredFiles.promiseIsAnyStoredFiles(library));
	}
}
