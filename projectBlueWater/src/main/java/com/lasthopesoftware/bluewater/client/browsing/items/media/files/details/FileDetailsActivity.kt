package com.lasthopesoftware.bluewater.client.browsing.items.media.files.details

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FormattedSessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.projectblue.shared.promises.extensions.toPromise

class FileDetailsActivity : AppCompatActivity() {

	private val lvFileDetails = LazyViewFinder<ListView>(this, R.id.lvFileDetails)
	private val pbLoadingFileDetails = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingFileDetails)
	private val pbLoadingFileThumbnail = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingFileThumbnail)
	private val fileNameTextViewFinder = LazyViewFinder<TextView>(this, R.id.tvFileName)
	private val artistTextViewFinder = LazyViewFinder<TextView>(this, R.id.tvArtist)
	private val fileThumbnailContainer = LazyViewFinder<RelativeLayout>(this, R.id.rlFileThumbnailContainer)

	private val imgFileThumbnailBuilder = object : AbstractSynchronousLazy<com.namehillsoftware.projectblue.shared.android.view.ScaledWrapImageView>() {
		override fun create(): com.namehillsoftware.projectblue.shared.android.view.ScaledWrapImageView {
			val rlFileThumbnailContainer = fileThumbnailContainer.findView()

			val imgFileThumbnail = com.namehillsoftware.projectblue.shared.android.view.ScaledWrapImageView(this@FileDetailsActivity)
			imgFileThumbnail.setBackgroundResource(R.drawable.drop_shadow)
			imgFileThumbnail.layoutParams = imgFileThumbnailLayoutParams.getObject()

			rlFileThumbnailContainer.addView(imgFileThumbnail)
			return imgFileThumbnail
		}
	}

	private val lazyImageProvider = lazy {
		val selectedLibraryIdentifierProvider: ISelectedLibraryIdentifierProvider = SelectedBrowserLibraryIdentifierProvider(this)
		ImageProvider(
			StaticLibraryIdentifierProvider(selectedLibraryIdentifierProvider),
			MemoryCachedImageAccess.getInstance(this))
	}

	private val defaultImageProvider = lazy { DefaultImageProvider(this) }
	private var fileKey = -1

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_view_file_details)
		setSupportActionBar(findViewById(R.id.fileDetailsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		fileKey = intent.getIntExtra(FILE_KEY, -1)
		setView(fileKey)
		NowPlayingFloatingActionButton.addNowPlayingFloatingActionButton(findViewById(R.id.viewFileDetailsRelativeLayout))
	}

	private fun setView(fileKey: Int) {
		if (fileKey < 0) {
			finish()
			return
		}

		lvFileDetails.findView().visibility = View.INVISIBLE
		pbLoadingFileDetails.findView().visibility = View.VISIBLE
		imgFileThumbnailBuilder.getObject().visibility = View.INVISIBLE
		pbLoadingFileThumbnail.findView().visibility = View.VISIBLE
		fileNameTextViewFinder.findView().text = getText(R.string.lbl_loading)
		artistTextViewFinder.findView().text = getText(R.string.lbl_loading)

		SessionConnection.getInstance(this).promiseSessionConnection()
			.then { c -> FormattedSessionFilePropertiesProvider(c, FilePropertyCache.getInstance()) }
			.eventually { f -> f.promiseFileProperties(ServiceFile(fileKey)) }
			.eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({ fileProperties ->
				setFileNameFromProperties(fileProperties)

				val artist = fileProperties[KnownFileProperties.ARTIST]
				artistTextViewFinder.findView().text = artist

				val filePropertyList = fileProperties.entries
					.filter { e -> !propertiesToSkip.contains(e.key) }
					.sortedBy { e -> e.key }

				lvFileDetails.findView().adapter = FileDetailsAdapter(this, R.id.linFileDetailsRow, filePropertyList)
				pbLoadingFileDetails.findView().visibility = View.INVISIBLE
				lvFileDetails.findView().visibility = View.VISIBLE
			}, this))
			.excuse(HandleViewIoException(this) { setView(fileKey) })
			.eventuallyExcuse(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response(com.namehillsoftware.projectblue.shared.exceptions.UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }

		lazyImageProvider.value
			.promiseFileBitmap(ServiceFile(fileKey))
			.eventually { bitmap ->
				bitmap?.toPromise() ?: defaultImageProvider.value.promiseFileBitmap()
			}
			.eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response({ result ->
				imgFileThumbnailBuilder.getObject().setImageBitmap(result)
				pbLoadingFileThumbnail.findView().visibility = View.INVISIBLE
				imgFileThumbnailBuilder.getObject().visibility = View.VISIBLE
			}, this))
	}

	private fun setFileNameFromProperties(fileProperties: Map<String, String>) {
		val fileName = fileProperties[KnownFileProperties.NAME] ?: return
		val fileNameTextView = fileNameTextViewFinder.findView()
		fileNameTextView.text = fileName
		fileNameTextView.postDelayed({ fileNameTextView.isSelected = true }, trackNameMarqueeDelay.toLong())
		val spannableString = SpannableString(getString(R.string.lbl_details).format(fileName))
		spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, fileName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		title = spannableString
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)

		// Update the intent
		setIntent(intent)
		fileKey = intent.getIntExtra(FILE_KEY, -1)
		setView(fileKey)
	}

	public override fun onStart() {
		super.onStart()
		restoreSessionConnection(this)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			finish()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	companion object {
		@JvmField
		val FILE_KEY: String = MagicPropertyBuilder.buildMagicPropertyName(FileDetailsActivity::class.java, "FILE_KEY")

		private const val trackNameMarqueeDelay = 1500

		private val propertiesToSkip = setOf(
			KnownFileProperties.AUDIO_ANALYSIS_INFO,
			KnownFileProperties.GET_COVER_ART_INFO,
			KnownFileProperties.IMAGE_FILE,
			KnownFileProperties.KEY,
			KnownFileProperties.STACK_FILES,
			KnownFileProperties.STACK_TOP,
			KnownFileProperties.STACK_VIEW,
			KnownFileProperties.WAVEFORM,
			KnownFileProperties.LengthInPcmBlocks)

		private val imgFileThumbnailLayoutParams: AbstractSynchronousLazy<RelativeLayout.LayoutParams> = object : AbstractSynchronousLazy<RelativeLayout.LayoutParams>() {
			override fun create(): RelativeLayout.LayoutParams {
				val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
				layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
				return layoutParams
			}
		}
	}
}
