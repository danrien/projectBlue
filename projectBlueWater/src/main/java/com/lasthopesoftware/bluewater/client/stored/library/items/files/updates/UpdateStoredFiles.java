package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface UpdateStoredFiles {
	Promise<StoredFile> promiseStoredFileUpdate(Library library, ServiceFile serviceFile);
}
