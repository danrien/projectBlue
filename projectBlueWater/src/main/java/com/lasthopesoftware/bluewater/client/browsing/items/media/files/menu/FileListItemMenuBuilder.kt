package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.graphics.Typeface
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.FilePlayClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.INowPlayingFileProvider
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise

class FileListItemMenuBuilder(private val serviceFiles: Collection<ServiceFile>, private val nowPlayingFileProvider: INowPlayingFileProvider, private val fileListItemNowPlayingRegistrar: FileListItemNowPlayingRegistrar)
	: AbstractFileListItemMenuBuilder<FileListItemMenuBuilder.ViewHolder>(R.layout.layout_file_item_menu) {

	override fun newViewHolder(fileItemMenu: FileListItemContainer) = ViewHolder(fileItemMenu)

	inner class ViewHolder(private val fileListItemContainer: FileListItemContainer) : RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {
		private val viewFileDetailsButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnViewFileDetails)
		private val playButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnPlaySong)
		private val addButtonFinder = LazyViewFinder<ImageButton>(itemView, R.id.btnAddToPlaylist)
		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.findTextView())

		private var fileListItemNowPlayingHandler: AutoCloseable? = null
		private var promisedTextViewUpdate: Promise<*>? = null

		fun update(positionedFile: PositionedFile) {
			val serviceFile = positionedFile.serviceFile

			promisedTextViewUpdate = fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)
			val textView = fileListItemContainer.findTextView()
			textView.setTypeface(null, Typeface.NORMAL)
			nowPlayingFileProvider.nowPlayingFile
				.eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({ f ->
					textView.setTypeface(null, com.namehillsoftware.projectblue.shared.android.view.ViewUtils.getActiveListItemTextViewStyle(serviceFile == f))
				}, textView.context))

			fileListItemNowPlayingHandler?.close()
			fileListItemNowPlayingHandler = fileListItemNowPlayingRegistrar.registerNewHandler(fileListItemContainer) { _, intent ->
				val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
				textView.setTypeface(null, com.namehillsoftware.projectblue.shared.android.view.ViewUtils.getActiveListItemTextViewStyle(serviceFile.key == fileKey))
			}

			val viewAnimator = fileListItemContainer.viewAnimator
			LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			playButtonFinder.findView().setOnClickListener(FilePlayClickListener(viewAnimator, positionedFile.playlistPosition, serviceFiles))
			viewFileDetailsButtonFinder.findView().setOnClickListener(ViewFileDetailsClickListener(viewAnimator, serviceFile))
			addButtonFinder.findView().setOnClickListener(AddClickListener(viewAnimator, serviceFile))
		}
	}

	private class AddClickListener(viewFlipper: NotifyOnFlipViewAnimator, private val serviceFile: ServiceFile) : AbstractMenuClickHandler(viewFlipper) {
		override fun onClick(v: View) {
			PlaybackService.addFileToPlaylist(v.context, serviceFile.key)
			super.onClick(v)
		}
	}
}
