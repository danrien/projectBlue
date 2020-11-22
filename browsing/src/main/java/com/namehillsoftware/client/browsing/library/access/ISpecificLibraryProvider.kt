package com.namehillsoftware.client.browsing.library.access

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 2/12/17.
 */
interface ISpecificLibraryProvider {
	val library: Promise<Library?>
}
