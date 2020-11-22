package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.activity

import android.content.*
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.ImageView.ScaleType
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertyHelpers
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.ImageProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.image.cache.MemoryCachedImageAccess
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.SpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.StaticLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.addOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.removeOnConnectionLostListener
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.list.NowPlayingFileListAdapter
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.shared.GenericBinder
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.projectblue.playback.file.PositionedFile
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

class NowPlayingActivity : AppCompatActivity(), IItemListMenuChangeHandler {

	companion object {
		private val logger = LoggerFactory.getLogger(NowPlayingActivity::class.java)
		@JvmStatic
		fun startNowPlayingActivity(context: Context) {
			val viewIntent = Intent(context, NowPlayingActivity::class.java)
			viewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
			context.startActivity(viewIntent)
		}

		private var isScreenKeptOn = false
		private var viewStructure: ViewStructure? = null
		private fun setRepeatingIcon(imageButton: ImageButton?, isRepeating: Boolean) {
			imageButton?.setImageDrawable(ViewUtils.getDrawable(imageButton.context, if (isRepeating) R.drawable.av_repeat_dark else R.drawable.av_no_repeat_dark))
		}
	}

	private var viewAnimator: ViewAnimator? = null
	private val messageHandler = lazy { Handler(mainLooper) }
	private val playButton = LazyViewFinder<ImageButton>(this, R.id.btnPlay)
	private val pauseButton = LazyViewFinder<ImageButton>(this, R.id.btnPause)
	private val songRating = LazyViewFinder<RatingBar>(this, R.id.rbSongRating)
	private val contentView = LazyViewFinder<RelativeLayout>(this, R.id.rlCtlNowPlaying)
	private val songProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbNowPlaying)
	private val nowPlayingImageViewFinder = LazyViewFinder<ImageView>(this, R.id.imgNowPlaying)
	private val nowPlayingArtist = LazyViewFinder<TextView>(this, R.id.tvSongArtist)
	private val isScreenKeptOnButton = LazyViewFinder<ImageButton>(this, R.id.isScreenKeptOnButton)
	private val nowPlayingTitle = LazyViewFinder<TextView>(this, R.id.tvSongTitle)
	private val nowPlayingImageLoading = LazyViewFinder<ImageView>(this, R.id.imgNowPlayingLoading)
	private val loadingProgressBar = LazyViewFinder<ProgressBar>(this, R.id.pbLoadingImg)
	private val viewNowPlayingListButton = LazyViewFinder<ImageButton>(this, R.id.viewNowPlayingListButton)
	private val drawerLayout = LazyViewFinder<DrawerLayout>(this, R.id.nowPlayingDrawer)

	private val localBroadcastManager = lazy { LocalBroadcastManager.getInstance(this) }

	private val nowPlayingToggledVisibilityControls = lazy {
		NowPlayingToggledVisibilityControls(LazyViewFinder(this@NowPlayingActivity, R.id.llNpButtons), LazyViewFinder(this@NowPlayingActivity, R.id.menuControlsLinearLayout), songRating)
	}

	private val lazyNowPlayingRepository = lazy {
		val libraryRepository = LibraryRepository(this)
		NowPlayingRepository(
			SpecificLibraryProvider(
				SelectedBrowserLibraryIdentifierProvider(this).selectedLibraryId!!,
				libraryRepository),
			libraryRepository)
	}

	private val lazyNowPlayingListAdapter = lazy {
		val nowPlayingFileListMenuBuilder = NowPlayingFileListItemMenuBuilder(
			lazyNowPlayingRepository.value,
			FileListItemNowPlayingRegistrar(localBroadcastManager.value))

		nowPlayingFileListMenuBuilder.setOnViewChangedListener(
			ViewChangedHandler()
				.setOnViewChangedListener(this)
				.setOnAnyMenuShown(this)
				.setOnAllMenusHidden(this))

		NowPlayingFileListAdapter(
			this,
			nowPlayingFileListMenuBuilder)
	}

	private val nowPlayingDrawerListView = lazy {
		val listView = findViewById<RecyclerView>(R.id.nowPlayingDrawerListView)
		listView.adapter = lazyNowPlayingListAdapter.value
		listView.layoutManager = LinearLayoutManager(this)
		listView
	}

	private val lazyImageProvider = lazy {
			SessionConnection.getInstance(this)
				.promiseSessionConnection()
				.then {
					ImageProvider(
						StaticLibraryIdentifierProvider(SelectedBrowserLibraryIdentifierProvider(this)),
						MemoryCachedImageAccess.getInstance(this))
				}
		}

	private val lazyDefaultImage = lazy { DefaultImageProvider(this).promiseFileBitmap() }

	private val drawerToggle = lazy {
		object : ActionBarDrawerToggle(
			this@NowPlayingActivity,  /* host Activity */
			drawerLayout.findView(),  /* DrawerLayout object */
			R.string.drawer_open,  /* "open drawer" description */
			R.string.drawer_close /* "close drawer" description */
		) {
			/** Called when a drawer has settled in a completely closed state.  */
			override fun onDrawerClosed(view: View) {
				super.onDrawerClosed(view)
				isDrawerOpened = false
			}

			/** Called when a drawer has settled in a completely open state.  */
			override fun onDrawerOpened(drawerView: View) {
				super.onDrawerOpened(drawerView)
				isDrawerOpened = true
				nowPlayingDrawerListView.value.bringToFront()
				drawerLayout.findView().requestLayout()
			}
		}
	}

	private val onConnectionLostListener = Runnable { WaitForConnectionDialog.show(this) }

	private val onPlaybackChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			if (!isDrawerOpened) updateNowPlayingListViewPosition()
			showNowPlayingControls()
			updateKeepScreenOnStatus()
			setView()
		}
	}

	private val onPlaybackStartedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			togglePlayingButtons(true)
			updateKeepScreenOnStatus()
		}
	}

	private val onPlaybackStoppedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			togglePlayingButtons(false)
			disableKeepScreenOn()
		}
	}

	private val onPlaylistChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			lazyNowPlayingRepository.value.nowPlaying
				.eventually { np ->
					lazyNowPlayingListAdapter.value
						.updateListEventually(np.playlist.mapIndexed { i, s -> PositionedFile(i, s) })
						.eventually<Unit>(LoopedInPromise.response(
							{
								if (!isDrawerOpened) updateNowPlayingListViewPosition()
								setView()
							}, messageHandler.value))
				}
		}
	}

	private val onTrackPositionChanged = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration, -1)
			if (fileDuration > -1) setTrackDuration(fileDuration)
			val filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1)
			if (filePosition > -1) setTrackProgress(filePosition)
		}
	}

	private var timerTask: TimerTask? = null
	private var isDrawerOpened = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_view_now_playing)

		nowPlayingToggledVisibilityControls.value.toggleVisibility(false)

		val playbackStoppedIntentFilter = IntentFilter()
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistPause)
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistStop)
		localBroadcastManager.value.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter)
		localBroadcastManager.value.registerReceiver(onPlaybackStartedReceiver, IntentFilter(PlaylistEvents.onPlaylistStart))
		localBroadcastManager.value.registerReceiver(onPlaybackChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistTrackChange))
		localBroadcastManager.value.registerReceiver(onPlaylistChangedReceiver, IntentFilter(PlaylistEvents.onPlaylistChange))
		localBroadcastManager.value.registerReceiver(onTrackPositionChanged, IntentFilter(TrackPositionBroadcaster.trackPositionUpdate))

		addOnConnectionLostListener(onConnectionLostListener)

		setNowPlayingBackgroundBitmap()

		contentView.findView().setOnClickListener { showNowPlayingControls() }

		playButton.findView().setOnClickListener { v: View ->
			if (!nowPlayingToggledVisibilityControls.value.isVisible) return@setOnClickListener
			PlaybackService.play(v.context)
			playButton.findView().visibility = View.INVISIBLE
			pauseButton.findView().visibility = View.VISIBLE
		}

		pauseButton.findView().setOnClickListener { v: View ->
			if (!nowPlayingToggledVisibilityControls.value.isVisible) return@setOnClickListener
			PlaybackService.pause(v.context)
			playButton.findView().visibility = View.VISIBLE
			pauseButton.findView().visibility = View.INVISIBLE
		}

		findViewById<ImageButton>(R.id.btnNext)?.setOnClickListener { v: View ->
			if (!nowPlayingToggledVisibilityControls.value.isVisible) return@setOnClickListener
			PlaybackService.next(v.context)
		}

		findViewById<ImageButton>(R.id.btnPrevious)?.setOnClickListener { v: View ->
			if (!nowPlayingToggledVisibilityControls.value.isVisible) return@setOnClickListener
			PlaybackService.previous(v.context)
		}

		val shuffleButton = findViewById<ImageButton>(R.id.repeatButton)
		setRepeatingIcon(shuffleButton)
		shuffleButton?.setOnClickListener { v: View ->
			lazyNowPlayingRepository.value
				.nowPlaying
				.eventually(LoopedInPromise.response({ result: NowPlaying ->
					val isRepeating = !result.isRepeating
					if (isRepeating) PlaybackService.setRepeating(v.context) else PlaybackService.setCompleting(v.context)
					setRepeatingIcon(shuffleButton, isRepeating)
					result
				}, messageHandler.value))
		}

		isScreenKeptOnButton.findView().setOnClickListener {
			isScreenKeptOn = !isScreenKeptOn
			updateKeepScreenOnStatus()
		}

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			songProgressBar.findView().progressDrawable.setColorFilter(resources.getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN)

		setupNowPlayingListDrawer()

		lazyNowPlayingRepository.value.nowPlaying
			.eventually { nowPlaying ->
				lazyNowPlayingListAdapter.value
					.updateListEventually(nowPlaying.playlist.mapIndexed { i, sf -> PositionedFile(i, sf) })
					.eventually<Unit>(LoopedInPromise.response({ updateNowPlayingListViewPosition() },	messageHandler.value))
			}
	}

	private fun setupNowPlayingListDrawer() {
		viewNowPlayingListButton.findView()
			.setOnClickListener { drawerLayout.findView().openDrawer(GravityCompat.END) }
		drawerLayout.findView().setScrimColor(ContextCompat.getColor(this, android.R.color.transparent))
		drawerLayout.findView().setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
		drawerLayout.findView().addDrawerListener(drawerToggle.value)
		val rotation = windowManager.defaultDisplay.rotation
		if (rotation != Surface.ROTATION_90) return
		val nowPlayingDrawerContainer = findViewById<LinearLayout>(R.id.nowPlayingDrawerContainer)
		val newLayoutParams = DrawerLayout.LayoutParams(nowPlayingDrawerContainer.layoutParams)
		newLayoutParams.gravity = GravityCompat.START
		nowPlayingDrawerContainer.layoutParams = newLayoutParams
		viewNowPlayingListButton.findView()
			.setOnClickListener { drawerLayout.findView().openDrawer(GravityCompat.START) }
		drawerLayout.findView().setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.END)
	}

	public override fun onStart() {
		super.onStart()
		updateKeepScreenOnStatus()
		val restore = restoreSessionConnection(this)
		if (!restore) initializeView()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) initializeView()
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun updateNowPlayingListViewPosition() {
		lazyNowPlayingRepository.value.nowPlaying
			.eventually<Unit>(LoopedInPromise.response(
				{ nowPlaying ->
					val newPosition = nowPlaying.playlistPosition
					if (newPosition > -1 && newPosition < nowPlaying.playlist.size) nowPlayingDrawerListView.value.scrollToPosition(newPosition)
				},
				messageHandler.value))
	}

	private fun setNowPlayingBackgroundBitmap() =
		lazyDefaultImage.value
			.eventually<Unit>(LoopedInPromise.response({ bitmap ->
				val nowPlayingImageLoadingView = nowPlayingImageLoading.findView()
				nowPlayingImageLoadingView.setImageBitmap(bitmap)
				nowPlayingImageLoadingView.scaleType = ScaleType.CENTER_CROP
			}, messageHandler.value))

	private fun initializeView() {
		playButton.findView().visibility = View.VISIBLE
		pauseButton.findView().visibility = View.INVISIBLE
		lazyNowPlayingRepository.value
			.nowPlaying
			.eventually { np ->
				SessionConnection.getInstance(this@NowPlayingActivity)
					.promiseSessionConnection()
					.eventually(LoopedInPromise.response<IConnectionProvider, Any?>({ connectionProvider: IConnectionProvider? ->
						val serviceFile = np.playlist[np.playlistPosition]
						val filePosition = if (connectionProvider != null && viewStructure != null && viewStructure!!.urlKeyHolder == UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile.key)) viewStructure!!.filePosition else np.filePosition
						setView(serviceFile, filePosition)
						null
					}, messageHandler.value))
			}
			.excuse { error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error) }
		bindService(Intent(this, PlaybackService::class.java), object : ServiceConnection {
			override fun onServiceConnected(name: ComponentName, service: IBinder) {
				togglePlayingButtons(((service as GenericBinder<*>).service as PlaybackService).isPlaying)
				unbindService(this)
			}

			override fun onServiceDisconnected(name: ComponentName) {}
		}, Context.BIND_AUTO_CREATE)
	}

	private fun setRepeatingIcon(imageButton: ImageButton?) {
		setRepeatingIcon(imageButton, false)
		lazyNowPlayingRepository.value
			.nowPlaying
			.eventually(LoopedInPromise.response<NowPlaying, Any?>({ result: NowPlaying? ->
				if (result != null) setRepeatingIcon(imageButton, result.isRepeating)
				null
			}, messageHandler.value))
	}

	private fun updateKeepScreenOnStatus() {
		isScreenKeptOnButton.findView().setImageDrawable(ViewUtils.getDrawable(this, if (isScreenKeptOn) R.drawable.ic_screen_on_white_36dp else R.drawable.ic_screen_off_white_36dp))
		if (isScreenKeptOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else disableKeepScreenOn()
	}

	private fun disableKeepScreenOn() {
		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	private fun togglePlayingButtons(isPlaying: Boolean) {
		playButton.findView().visibility = ViewUtils.getVisibility(!isPlaying)
		pauseButton.findView().visibility = ViewUtils.getVisibility(isPlaying)
	}

	private fun setView() {
		lazyNowPlayingRepository.value
			.nowPlaying
			.eventually { np ->
				SessionConnection.getInstance(this)
					.promiseSessionConnection()
					.eventually(LoopedInPromise.response({ connectionProvider: IConnectionProvider ->
						if (np.playlistPosition >= np.playlist.size) return@response
						val serviceFile = np.playlist[np.playlistPosition]
						val filePosition = if (viewStructure?.urlKeyHolder == UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile.key)) viewStructure?.filePosition ?: 0 else 0
						setView(serviceFile, filePosition)
					}, messageHandler.value))
			}
			.excuse { e -> logger.error("An error occurred while getting the Now Playing data", e) }
	}

	private fun setView(serviceFile: ServiceFile, initialFilePosition: Long) {
		SessionConnection.getInstance(this)
			.promiseSessionConnection()
			.eventually(LoopedInPromise.response(ImmediateResponse { connectionProvider: IConnectionProvider ->
				val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile.key)
				if (viewStructure?.urlKeyHolder != urlKeyHolder) {
					viewStructure?.release()
					viewStructure = null
				}

				val localViewStructure = viewStructure ?: ViewStructure(urlKeyHolder, serviceFile)
				viewStructure = localViewStructure

				setNowPlayingImage(localViewStructure, serviceFile)

				if (localViewStructure.fileProperties != null) {
					setFileProperties(serviceFile, initialFilePosition, localViewStructure.fileProperties)
					return@ImmediateResponse
				}

				disableViewWithMessage()
				val sessionFilePropertiesProvider = SessionFilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance())
				sessionFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually { fileProperties ->
						if (localViewStructure !== viewStructure) Promise.empty<Unit>()
						else LoopedInPromise(MessageWriter<Unit> {
							localViewStructure.fileProperties = fileProperties.toMutableMap()
							setFileProperties(serviceFile, initialFilePosition, fileProperties)
						}, messageHandler.value)
					}
					.excuse { e: Throwable -> LoopedInPromise.response({ exception: Throwable -> handleIoException(serviceFile, initialFilePosition, exception) }, messageHandler.value).promiseResponse(e) }
			}, messageHandler.value))
	}

	private fun setNowPlayingImage(viewStructure: ViewStructure, serviceFile: ServiceFile) {
		val nowPlayingImage = nowPlayingImageViewFinder.findView()
		loadingProgressBar.findView().visibility = View.VISIBLE
		nowPlayingImage.visibility = View.INVISIBLE
		if (viewStructure.promisedNowPlayingImage == null) {
			viewStructure.promisedNowPlayingImage = lazyImageProvider.value.eventually { provider -> provider.promiseFileBitmap(serviceFile) }
		}
		viewStructure.promisedNowPlayingImage
			?.eventually<Unit> { bitmap ->
				if (viewStructure !== Companion.viewStructure) Promise.empty()
				else LoopedInPromise(MessageWriter { setNowPlayingImage(bitmap) }, messageHandler.value)
			}
			?.excuse { e ->
				if (e is CancellationException)	logger.info("Bitmap retrieval cancelled", e)
				else logger.error("There was an error retrieving the image for serviceFile $serviceFile", e)
			}
	}

	private fun setNowPlayingImage(bitmap: Bitmap?) {
		nowPlayingImageViewFinder.findView().setImageBitmap(bitmap)
		loadingProgressBar.findView().visibility = View.INVISIBLE
		if (bitmap != null) displayImageBitmap()
	}

	private fun setFileProperties(serviceFile: ServiceFile, initialFilePosition: Long, fileProperties: Map<String, String>?): Void? {
		val artist = fileProperties!![KnownFileProperties.ARTIST]
		nowPlayingArtist.findView().text = artist
		val title = fileProperties[KnownFileProperties.NAME]
		nowPlayingTitle.findView().text = title
		nowPlayingTitle.findView().isSelected = true
		var fileRating: Float? = null
		val stringRating = fileProperties[KnownFileProperties.RATING]
		try {
			if (stringRating != null && stringRating.isNotEmpty()) fileRating = java.lang.Float.valueOf(stringRating)
		} catch (e: NumberFormatException) {
			logger.info("Failed to parse rating", e)
		}
		setFileRating(serviceFile, fileRating)
		val duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties)
		setTrackDuration(if (duration > 0) duration.toLong() else 100.toLong())
		setTrackProgress(initialFilePosition)
		return null
	}

	private fun setFileRating(serviceFile: ServiceFile, rating: Float?) {
		val songRatingBar = songRating.findView()
		songRatingBar.rating = rating ?: 0f
		songRatingBar.onRatingBarChangeListener = OnRatingBarChangeListener { _, newRating, fromUser ->
			if (fromUser && nowPlayingToggledVisibilityControls.value.isVisible) {
				val stringRating = newRating.roundToInt().toString()
				SessionConnection.getInstance(this).promiseSessionConnection()
					.then { c -> FilePropertiesStorage.storeFileProperty(c, FilePropertyCache.getInstance(), serviceFile, KnownFileProperties.RATING, stringRating, false) }
				viewStructure?.fileProperties?.put(KnownFileProperties.RATING, stringRating)
			}
		}
		songRatingBar.isEnabled = true
	}

	private fun setTrackDuration(duration: Long) {
		songProgressBar.findView().max = duration.toInt()
		viewStructure?.fileDuration = duration
	}

	private fun setTrackProgress(progress: Long) {
		songProgressBar.findView().progress = progress.toInt()
		viewStructure?.filePosition = progress
	}

	private fun handleIoException(serviceFile: ServiceFile, position: Long, exception: Throwable): Boolean {
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) {
			resetViewOnReconnect(serviceFile, position)
			return true
		}
		UnexpectedExceptionToaster.announce(this, exception)
		return false
	}

	private fun displayImageBitmap() {
		val nowPlayingImage = nowPlayingImageViewFinder.findView()
		nowPlayingImage.scaleType = ScaleType.CENTER_CROP
		nowPlayingImage.visibility = View.VISIBLE
	}

	private fun showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.value.toggleVisibility(true)
		contentView.findView().invalidate()
		timerTask?.cancel()
		val newTimerTask = object : TimerTask() {
			var cancelled = false
			override fun run() {
				if (!cancelled) nowPlayingToggledVisibilityControls.value.toggleVisibility(false)
			}

			override fun cancel(): Boolean {
				cancelled = true
				return super.cancel()
			}
		}.apply { timerTask = this }
		messageHandler.value.postDelayed(newTimerTask, 5000)
	}

	private fun resetViewOnReconnect(serviceFile: ServiceFile, position: Long) {
		pollSessionConnection(this).then {
			if (serviceFile == viewStructure?.serviceFile) {
				viewStructure?.promisedNowPlayingImage!!.cancel()
				viewStructure?.promisedNowPlayingImage = null
			}
			setView(serviceFile, position)
		}
		WaitForConnectionDialog.show(this)
	}

	private fun disableViewWithMessage() {
		nowPlayingTitle.findView().setText(R.string.lbl_loading)
		nowPlayingArtist.findView().text = ""
		songRating.findView().rating = 0f
		songRating.findView().isEnabled = false
	}

	override fun onStop() {
		super.onStop()
		disableKeepScreenOn()
	}

	public override fun onDestroy() {
		super.onDestroy()
		timerTask?.cancel()
		localBroadcastManager.value.unregisterReceiver(onPlaybackStoppedReceiver)
		localBroadcastManager.value.unregisterReceiver(onPlaybackStartedReceiver)
		localBroadcastManager.value.unregisterReceiver(onPlaybackChangedReceiver)
		localBroadcastManager.value.unregisterReceiver(onPlaylistChangedReceiver)
		localBroadcastManager.value.unregisterReceiver(onTrackPositionChanged)
		removeOnConnectionLostListener(onConnectionLostListener)
	}

	override fun onAllMenusHidden() {}
	override fun onAnyMenuShown() {}

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		if (isDrawerOpened) {
			drawerLayout.findView().closeDrawers()
			return
		}
		super.onBackPressed()
	}

	private class ViewStructure(val urlKeyHolder: UrlKeyHolder<Int>, val serviceFile: ServiceFile) {
		var fileProperties: MutableMap<String, String>? = null
		var promisedNowPlayingImage: Promise<Bitmap?>? = null
		var filePosition: Long = 0
		var fileDuration: Long = 0

		fun release() {
			promisedNowPlayingImage?.cancel()
		}
	}
}
