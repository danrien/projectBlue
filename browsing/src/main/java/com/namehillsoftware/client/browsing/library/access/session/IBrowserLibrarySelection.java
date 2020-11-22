package com.namehillsoftware.client.browsing.library.access.session;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/19/17.
 */

public interface IBrowserLibrarySelection {
	Promise<Library> selectBrowserLibrary(LibraryId libraryId);
}
