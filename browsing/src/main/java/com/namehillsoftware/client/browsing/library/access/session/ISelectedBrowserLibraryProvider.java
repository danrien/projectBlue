package com.namehillsoftware.client.browsing.library.access.session;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

/**
 * Created by david on 2/21/17.
 */
public interface ISelectedBrowserLibraryProvider {
	Promise<Library> getBrowserLibrary();
}
