package com.lasthopesoftware.bluewater.permissions.read.specs;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider;

import junit.framework.TestCase;

/**
 * Created by david on 7/10/16.
 */
public class GivenLibraryNotRequestingStorageReadPermissions {
	public static class AndOsGrantingPermissions extends TestCase {

		private boolean isPermissionGranted;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider
					= new ApplicationReadPermissionsRequirementsProvider(library -> false, () -> true);

			this.isPermissionGranted = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(new Library());
		}

		public final void testThatPermissionsIsNotRequired() {
			assertFalse(this.isPermissionGranted);
		}
	}

	public static class AndOsNotGrantingPermissions extends TestCase {

		private boolean isPermissionGranted;

		protected void setUp() throws Exception {
			super.setUp();

			final ApplicationReadPermissionsRequirementsProvider applicationReadPermissionsRequirementsProvider
					= new ApplicationReadPermissionsRequirementsProvider(library -> false, () -> false);

			this.isPermissionGranted = applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(new Library());
		}

		public final void testThatPermissionsIsNotRequired() {
			assertFalse(this.isPermissionGranted);
		}
	}
}