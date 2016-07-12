package com.lasthopesoftware.bluewater.permissions.write;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.ILibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.client.library.repository.permissions.write.LibraryStorageWritePermissionsRequirementsProvider;
import com.lasthopesoftware.permissions.storage.write.ExternalStorageWritePermissionsArbitratorForOs;
import com.lasthopesoftware.permissions.storage.write.IStorageWritePermissionArbitratorForOs;

/**
 * Created by david on 7/3/16.
 */
public class ApplicationWritePermissionsRequirementsProvider implements IApplicationWritePermissionsRequirementsProvider {

	private final ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider;
	private final IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs;

	public ApplicationWritePermissionsRequirementsProvider(Context context, Library library) {
		this(new LibraryStorageWritePermissionsRequirementsProvider(library), new ExternalStorageWritePermissionsArbitratorForOs(context));
	}

	public ApplicationWritePermissionsRequirementsProvider(
			ILibraryStorageWritePermissionsRequirementsProvider storageWritePermissionsRequirementsProvider,
			IStorageWritePermissionArbitratorForOs storageWritePermissionArbitratorForOs) {

		this.storageWritePermissionsRequirementsProvider = storageWritePermissionsRequirementsProvider;
		this.storageWritePermissionArbitratorForOs = storageWritePermissionArbitratorForOs;
	}

	@Override
	public boolean isWritePermissionsRequired() {
		return storageWritePermissionsRequirementsProvider.isWritePermissionsRequired() && !storageWritePermissionArbitratorForOs.isWritePermissionGranted();
	}
}
