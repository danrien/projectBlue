package com.namehillsoftware.client.browsing.library.repository.storage.read;


import com.namehillsoftware.client.browsing.library.repository.Library;

/**
 * Created by david on 7/3/16.
 */
public interface ILibraryStorageReadPermissionsRequirementsProvider {
	boolean isReadPermissionsRequiredForLibrary(@NonNull Library library);
}
