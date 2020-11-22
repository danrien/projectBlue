package com.lasthopesoftware.bluewater.client.browsing.library.views.access

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.views.DownloadViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryViewProvider(
        private val selectedLibrary: com.namehillsoftware.client.browsing.library.access.session.ISelectedBrowserLibraryProvider,
        private val libraryViews: ProvideLibraryViews,
        private val libraryStorage: ILibraryStorage) : ProvideSelectedLibraryView {

	override fun promiseSelectedOrDefaultView(): Promise<ViewItem?> {
		val promisedSelectedLibrary = selectedLibrary.browserLibrary
		return libraryViews.promiseLibraryViews().eventually { views ->
			when {
				views.isNotEmpty() -> {
					promisedSelectedLibrary.eventually { library ->
						when {
							library.selectedViewType == Library.ViewType.DownloadView -> Promise(DownloadViewItem() as ViewItem)
							library.selectedView > 0 -> Promise(views.first { it.key == library.selectedView })
							else -> {
								val selectedView = views.first()
								library
									.setSelectedView(selectedView.key)
									.setSelectedViewType(Library.ViewType.StandardServerView)
								libraryStorage.saveLibrary(library).then { selectedView }
							}
						}
					}
				}
				else ->	Promise.empty()
			}
		}
	}
}
