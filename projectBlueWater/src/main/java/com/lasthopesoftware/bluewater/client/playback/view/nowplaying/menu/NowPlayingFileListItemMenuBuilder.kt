package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu

import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.AbstractFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileNameTextViewSetter
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.listeners.FileSeekToClickListener
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.listeners.RemovePlaylistFileClickListener
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.projectblue.playback.file.PositionedFile


class NowPlayingFileListItemMenuBuilder(private val nowPlayingRepository: INowPlayingRepository, private val fileListItemNowPlayingRegistrar: FileListItemNowPlayingRegistrar)
	: AbstractFileListItemMenuBuilder<NowPlayingFileListItemMenuBuilder.ViewHolder>(R.layout.layout_now_playing_file_item_menu) {

	override fun newViewHolder(fileItemMenu: FileListItemContainer) = ViewHolder(fileItemMenu)

	inner class ViewHolder internal constructor(private val fileListItemContainer: FileListItemContainer)
		: RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {

		private val viewFileDetailsButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val removeButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnRemoveFromPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.findTextView())

		var fileListItemNowPlayingHandler: AutoCloseable? = null

		fun update(positionedFile: PositionedFile) {
			val fileListItem = fileListItemContainer
			val textView = fileListItem.findTextView()

			val serviceFile = positionedFile.serviceFile

			fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			val position = positionedFile.playlistPosition

			val viewFlipper = fileListItem.viewAnimator

			nowPlayingRepository
				.nowPlaying
				.eventually(LoopedInPromise.response({ np ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == np.playlistPosition))
					viewFlipper.isSelected = position == np.playlistPosition
				}, textView.context))

			fileListItemNowPlayingHandler?.close()
			fileListItemNowPlayingHandler = fileListItemNowPlayingRegistrar.registerNewHandler(fileListItem) { _, intent ->
				val playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1)
				textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(position == playlistPosition))
				viewFlipper.isSelected = position == playlistPosition
			}

			LongClickViewAnimatorListener.tryFlipToPreviousView(viewFlipper)
			playButtonFinder.findView().setOnClickListener(FileSeekToClickListener(viewFlipper, position))
			viewFileDetailsButtonFinder.findView().setOnClickListener(ViewFileDetailsClickListener(viewFlipper, serviceFile))
			removeButtonFinder.findView().setOnClickListener(RemovePlaylistFileClickListener(viewFlipper, position))
		}
	}
}
