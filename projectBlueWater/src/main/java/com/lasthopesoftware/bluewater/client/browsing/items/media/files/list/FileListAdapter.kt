package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.content.Context
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileDiffer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.INowPlayingFileProvider
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

internal class FileListAdapter(context: Context, serviceFiles: List<ServiceFile>, itemListMenuChangeHandler: IItemListMenuChangeHandler?, nowPlayingFileProvider: INowPlayingFileProvider)
	: DeferredListAdapter<ServiceFile, FileListItemMenuBuilder.ViewHolder>(context, ServiceFileDiffer) {

	private val fileListItemMenuBuilder = FileListItemMenuBuilder(serviceFiles, nowPlayingFileProvider)

	init {
		val viewChangedHandler = ViewChangedHandler()
		viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler)
		viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler)
		viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler)
		fileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListItemMenuBuilder.ViewHolder =
		fileListItemMenuBuilder.getView(parent)

	override fun onBindViewHolder(holder: FileListItemMenuBuilder.ViewHolder, position: Int) =
		holder.update(position)
}
