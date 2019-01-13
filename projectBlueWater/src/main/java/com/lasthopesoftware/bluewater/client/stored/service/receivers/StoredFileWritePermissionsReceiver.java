package com.lasthopesoftware.bluewater.client.stored.service.receivers;

import com.lasthopesoftware.bluewater.client.library.permissions.storage.request.write.IStorageWritePermissionsRequestedBroadcaster;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization;
import com.lasthopesoftware.storage.write.permissions.IStorageWritePermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public class StoredFileWritePermissionsReceiver implements ReceiveStoredFileEvent, ImmediateResponse<StoredFile, Void> {

	private final IStorageWritePermissionArbitratorForOs writePermissionArbitratorForOs;
	private final IStorageWritePermissionsRequestedBroadcaster writePermissionsRequestedBroadcaster;
	private final IStoredFileAccess storedFileAccess;

	public StoredFileWritePermissionsReceiver(
		IStorageWritePermissionArbitratorForOs writePermissionArbitratorForOs,
		IStorageWritePermissionsRequestedBroadcaster writePermissionsRequestedBroadcaster,
		IStoredFileAccess storedFileAccess) {
		this.writePermissionArbitratorForOs = writePermissionArbitratorForOs;
		this.writePermissionsRequestedBroadcaster = writePermissionsRequestedBroadcaster;
		this.storedFileAccess = storedFileAccess;
	}

	@Override
	public void receive(int storedFileId) {
		if (writePermissionArbitratorForOs.isWritePermissionGranted()) return;

		storedFileAccess.getStoredFile(storedFileId).then(this);
	}

	@Override
	public boolean IsAcceptable(String event) {
		return StoredFileSynchronization.onFileWriteErrorEvent.equals(event);
	}

	@Override
	public Void respond(StoredFile storedFile) {
		writePermissionsRequestedBroadcaster.sendWritePermissionsNeededBroadcast(storedFile.getLibraryId());
		return null;
	}
}
