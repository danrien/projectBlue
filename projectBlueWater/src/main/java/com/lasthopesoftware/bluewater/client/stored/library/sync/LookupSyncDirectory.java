package com.lasthopesoftware.bluewater.client.stored.library.sync;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public interface LookupSyncDirectory {
	Promise<File> promiseSyncDirectory(Library library);
}
