package com.lasthopesoftware.bluewater.permissions;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.permissions.ILibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.servers.library.repository.permissions.LibraryStorageReadPermissionsRequirementsProvider;
import com.lasthopesoftware.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.permissions.IStorageReadPermissionArbitratorForOs;

/**
 * Created by david on 7/3/16.
 */
public class ApplicationReadPermissionsRequirementsProvider implements IApplicationReadPermissionsRequirementsProvider {

	private final ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider;
	private final IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs;

	public ApplicationReadPermissionsRequirementsProvider(Context context, Library library) {
		this(new LibraryStorageReadPermissionsRequirementsProvider(library), new ExternalStorageReadPermissionsArbitratorForOs(context));
	}

	public ApplicationReadPermissionsRequirementsProvider(ILibraryStorageReadPermissionsRequirementsProvider libraryStorageReadPermissionsRequirementsProvider,
	                                                      IStorageReadPermissionArbitratorForOs storageReadPermissionArbitratorForOs) {

		this.libraryStorageReadPermissionsRequirementsProvider = libraryStorageReadPermissionsRequirementsProvider;
		this.storageReadPermissionArbitratorForOs = storageReadPermissionArbitratorForOs;
	}

	@Override
	public boolean isReadPermissionsRequired() {
		return libraryStorageReadPermissionsRequirementsProvider.isReadPermissionsRequired() && !storageReadPermissionArbitratorForOs.isReadPermissionGranted();
	}
}
