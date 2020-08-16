package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.FilePlayClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.ViewFileDetailsClickListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.AbstractViewChangedListenerContainer
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.NotifyOnFlipViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.AbstractMenuClickHandler
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.INowPlayingFileProvider
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class FileListItemMenuBuilder(private val serviceFiles: List<ServiceFile>, private val nowPlayingFileProvider: INowPlayingFileProvider) : AbstractViewChangedListenerContainer() {

	fun getView(parent: ViewGroup): ViewHolder {
		val fileItemMenu = FileListItemContainer(parent.context)
		return ViewHolder(fileItemMenu)
	}

	inner class ViewHolder internal constructor(val fileListItemContainer: FileListItemContainer) : RecyclerView.ViewHolder(fileListItemContainer.viewAnimator) {
		private var fileListItemNowPlayingHandler: AbstractFileListItemNowPlayingHandler? = null
		private var promisedTextViewUpdate: Promise<*>? = null

		private val fileNameTextViewSetter = FileNameTextViewSetter(fileListItemContainer.findTextView())
		private val viewFileDetailsButtonFinder: LazyViewFinder<ImageButton>
		private val playButtonFinder: LazyViewFinder<ImageButton>
		private val addButtonFinder: LazyViewFinder<ImageButton>
		private val textView = fileListItemContainer.findTextView()

		init {
			val notifyOnFlipViewAnimator = fileListItemContainer.viewAnimator
			val inflater = notifyOnFlipViewAnimator.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
			val fileMenu = inflater.inflate(R.layout.layout_file_item_menu, notifyOnFlipViewAnimator, false) as LinearLayout
			viewFileDetailsButtonFinder = LazyViewFinder(fileMenu, R.id.btnViewFileDetails)
			playButtonFinder = LazyViewFinder(fileMenu, R.id.btnPlaySong)
			addButtonFinder = LazyViewFinder(fileMenu, R.id.btnAddToPlaylist)

			notifyOnFlipViewAnimator.addView(fileMenu)
			notifyOnFlipViewAnimator.setViewChangedListener(getOnViewChangedListener())
		}

		fun update(position: Int) {
			val serviceFile = serviceFiles[position]

			promisedTextViewUpdate = fileNameTextViewSetter.promiseTextViewUpdate(serviceFile)

			textView.setTypeface(null, Typeface.NORMAL)
			nowPlayingFileProvider
				.nowPlayingFile
				.eventually<Unit>(LoopedInPromise.response({ f ->
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile == f))
				}, textView.context))

			fileListItemNowPlayingHandler?.release()
			fileListItemNowPlayingHandler = object : AbstractFileListItemNowPlayingHandler(fileListItemContainer) {
				override fun onReceive(context: Context, intent: Intent) {
					val fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1)
					textView.setTypeface(null, ViewUtils.getActiveListItemTextViewStyle(serviceFile.key == fileKey))
				}
			}

			val viewAnimator = fileListItemContainer.viewAnimator
			LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)
			playButtonFinder.findView().setOnClickListener(FilePlayClickListener(viewAnimator, position, serviceFiles))
			viewFileDetailsButtonFinder.findView().setOnClickListener(ViewFileDetailsClickListener(viewAnimator, serviceFile))
			addButtonFinder.findView().setOnClickListener(AddClickListener(viewAnimator, serviceFile))
		}
	}

	private class AddClickListener internal constructor(viewFlipper: NotifyOnFlipViewAnimator?, private val mServiceFile: ServiceFile) : AbstractMenuClickHandler(viewFlipper) {
		override fun onClick(view: View) {
			PlaybackService.addFileToPlaylist(view.context, mServiceFile.key)
			super.onClick(view)
		}
	}
}
