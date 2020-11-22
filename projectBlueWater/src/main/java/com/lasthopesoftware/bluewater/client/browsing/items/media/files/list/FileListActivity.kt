package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.IItemListViewContainer
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.FileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu.FileListItemNowPlayingRegistrar
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.connection.HandleViewIoException
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity.Companion.restoreSessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFileProvider.Companion.fromActiveLibrary
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.NowPlayingFloatingActionButton.Companion.addNowPlayingFloatingActionButton
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise

class FileListActivity :
	AppCompatActivity(),
	IItemListViewContainer,
	ImmediateResponse<List<ServiceFile>?, Unit>,
	Runnable,
	() -> PromisedResponse<List<ServiceFile>?, Unit> {

	private lateinit var nowPlayingFloatingActionButton: NowPlayingFloatingActionButton

	private var itemId = 0
	private val pbLoading = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val fileListView = LazyViewFinder<RecyclerView>(this, R.id.loadedRecyclerView)

	private val onFileProviderComplete = lazy(this)
	private var viewAnimator: ViewAnimator? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_list_view)
		setSupportActionBar(findViewById(R.id.listViewToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		fileListView.findView().visibility = View.INVISIBLE
		pbLoading.findView().visibility = View.VISIBLE

		if (savedInstanceState != null) itemId = savedInstanceState.getInt(key)
		if (itemId == 0) itemId = this.intent.getIntExtra(key, 1)

		title = intent.getStringExtra(value)
		nowPlayingFloatingActionButton = addNowPlayingFloatingActionButton(findViewById(R.id.asynchronousRecyclerViewContainer))

		run()
	}

	override fun run() {
		getInstance(this).promiseSessionConnection()
			.eventually { connection ->
				val parameters = FileListParameters.getInstance().getFileListParameters(Item(itemId))
				val stringListProvider = FileStringListProvider(connection)
				val fileProvider = FileProvider(stringListProvider)
				fileProvider.promiseFiles(FileListParameters.Options.None, *parameters)
			}
			.eventually(onFileProviderComplete.value)
			.excuse(HandleViewIoException(this, this))
			.eventuallyExcuse(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response(com.namehillsoftware.projectblue.shared.exceptions.UnexpectedExceptionToasterResponse(this), this))
			.then { finish() }
	}

	override fun invoke(): PromisedResponse<List<ServiceFile>?, Unit> = com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response(this, this)

	override fun respond(serviceFiles: List<ServiceFile>?) {
		if (serviceFiles == null) return

		val nowPlayingFileProvider = fromActiveLibrary(this) ?: return

		val fileListItemMenuBuilder = FileListItemMenuBuilder(
			serviceFiles,
			nowPlayingFileProvider,
			FileListItemNowPlayingRegistrar(LocalBroadcastManager.getInstance(this)))

		ItemListMenuChangeHandler(this).apply {
			fileListItemMenuBuilder.setOnViewChangedListener(
				ViewChangedHandler()
					.setOnViewChangedListener(this)
					.setOnAnyMenuShown(this)
					.setOnAllMenusHidden(this))
		}

		val fileListView = fileListView.findView()
		fileListView.adapter = FileListAdapter(serviceFiles, fileListItemMenuBuilder)
		val layoutManager = LinearLayoutManager(this)
		fileListView.layoutManager = layoutManager
		fileListView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
		fileListView.visibility = View.VISIBLE

		pbLoading.findView().visibility = View.INVISIBLE
	}

	public override fun onStart() {
		super.onStart()
		restoreSessionConnection(this)
	}

	public override fun onSaveInstanceState(savedInstanceState: Bundle) {
		super.onSaveInstanceState(savedInstanceState)
		savedInstanceState.putInt(key, itemId)
	}

	public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
		super.onRestoreInstanceState(savedInstanceState)
		itemId = savedInstanceState.getInt(key)
	}

	override fun onCreateOptionsMenu(menu: Menu) = com.namehillsoftware.projectblue.shared.android.view.ViewUtils.buildStandardMenu(this, menu)

	override fun onOptionsItemSelected(item: MenuItem) =
		com.namehillsoftware.projectblue.shared.android.view.ViewUtils.handleNavMenuClicks(this, item) || super.onOptionsItemSelected(item)

	override fun onBackPressed() {
		if (LongClickViewAnimatorListener.tryFlipToPreviousView(viewAnimator)) return
		super.onBackPressed()
	}

	override fun updateViewAnimator(viewAnimator: ViewAnimator) {
		this.viewAnimator = viewAnimator
	}

	override fun getNowPlayingFloatingActionButton() = nowPlayingFloatingActionButton

	companion object {
		private val magicPropertyBuilder = MagicPropertyBuilder(FileListActivity::class.java)
		private val key = magicPropertyBuilder.buildProperty("key")
		private val value = magicPropertyBuilder.buildProperty("value")
		@JvmStatic
		fun startFileListActivity(context: Context, item: IItem) {
			val fileListIntent = Intent(context, FileListActivity::class.java)
			fileListIntent.putExtra(key, item.key)
			fileListIntent.putExtra(value, item.value)
			context.startActivity(fileListIntent)
		}
	}
}
