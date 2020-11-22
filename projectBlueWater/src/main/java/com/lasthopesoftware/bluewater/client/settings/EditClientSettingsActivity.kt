package com.lasthopesoftware.bluewater.client.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.RemoveLibraryConfirmationDialogBuilder
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import java.util.*

class EditClientSettingsActivity : AppCompatActivity() {
	private val saveButton = LazyViewFinder<Button>(this, R.id.btnConnect)
	private val txtAccessCode = LazyViewFinder<EditText>(this, R.id.txtAccessCode)
	private val txtUserName = LazyViewFinder<EditText>(this, R.id.txtUserName)
	private val txtPassword = LazyViewFinder<EditText>(this, R.id.txtPassword)
	private val txtSyncPath = LazyViewFinder<TextView>(this, R.id.txtSyncPath)
	private val chkLocalOnly = LazyViewFinder<CheckBox>(this, R.id.chkLocalOnly)
	private val rgSyncFileOptions = LazyViewFinder<RadioGroup>(this, R.id.rgSyncFileOptions)
	private val chkIsUsingExistingFiles = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingExistingFiles)
	private val chkIsUsingLocalConnectionForSync = LazyViewFinder<CheckBox>(this, R.id.chkIsUsingLocalConnectionForSync)
	private val chkIsWakeOnLanEnabled = LazyViewFinder<CheckBox>(this, R.id.isWakeOnLan)
	private val applicationWritePermissionsRequirementsProviderLazy = lazy { ApplicationWritePermissionsRequirementsProvider(this) }
	private val applicationReadPermissionsRequirementsProviderLazy = lazy { ApplicationReadPermissionsRequirementsProvider(this) }
	private val lazyLibraryProvider = lazy { LibraryRepository(this) }
	private val settingsMenu = lazy {
		EditClientSettingsMenu(
			this,
			AboutTitleBuilder(this),
			RemoveLibraryConfirmationDialogBuilder(
				this,
				LibraryRemoval(
					StoredItemAccess(this),
					lazyLibraryProvider.value,
					SelectedBrowserLibraryIdentifierProvider(this),
					lazyLibraryProvider.value,
                        com.namehillsoftware.client.browsing.library.access.session.BrowserLibrarySelection(this, LocalBroadcastManager.getInstance(this), lazyLibraryProvider.value))))
	}
	private var library: Library? = null

	private val connectionButtonListener = View.OnClickListener {
		saveButton.findView().isEnabled = false

		val localLibrary = library ?: Library(_nowPlayingId = -1)

		library = localLibrary
			.setAccessCode(txtAccessCode.findView().text.toString())
			.setUserName(txtUserName.findView().text.toString())
			.setPassword(txtPassword.findView().text.toString())
			.setLocalOnly(chkLocalOnly.findView().isChecked)
			.setCustomSyncedFilesPath(txtSyncPath.findView().text.toString())
			.setSyncedFileLocation(when (rgSyncFileOptions.findView().checkedRadioButtonId) {
				R.id.rbPublicLocation -> SyncedFileLocation.EXTERNAL
				R.id.rbPrivateToApp -> SyncedFileLocation.INTERNAL
				R.id.rbCustomLocation -> SyncedFileLocation.CUSTOM
				else -> null
			})
			.setIsUsingExistingFiles(chkIsUsingExistingFiles.findView().isChecked)
			.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.findView().isChecked)
			.setIsWakeOnLanEnabled(chkIsWakeOnLanEnabled.findView().isChecked)

		val permissionsToRequest = ArrayList<String>(2)
		if (applicationReadPermissionsRequirementsProviderLazy.value.isReadPermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		if (applicationWritePermissionsRequirementsProviderLazy.value.isWritePermissionsRequiredForLibrary(localLibrary))
			permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		if (permissionsToRequest.size > 0) {
			val permissionsToRequestArray = permissionsToRequest.toTypedArray()
			ActivityCompat.requestPermissions(this@EditClientSettingsActivity, permissionsToRequestArray, permissionsRequestInteger)
		} else {
			saveLibraryAndFinish()
		}
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_server_settings)
		setSupportActionBar(findViewById(R.id.serverSettingsToolbar))
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		saveButton.findView().setOnClickListener(connectionButtonListener)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = settingsMenu.value.buildSettingsMenu(menu)

	override fun onStart() {
		super.onStart()
		initializeLibrary(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		initializeLibrary(intent)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode != selectDirectoryResultId) {
			super.onActivityResult(requestCode, resultCode, data)
			return
		}
		val uri = data?.dataString
		if (uri != null) txtSyncPath.findView().text = uri
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean =
		settingsMenu.value.handleSettingsMenuClicks(item, library)

	private fun initializeLibrary(intent: Intent) {
		val externalFilesDir = Environment.getExternalStorageDirectory()
		val syncPathTextView = txtSyncPath.findView()
		if (externalFilesDir != null) syncPathTextView.text = externalFilesDir.path

		val syncFilesRadioGroup = rgSyncFileOptions.findView()
		syncFilesRadioGroup.check(R.id.rbPrivateToApp)
		syncFilesRadioGroup.setOnCheckedChangeListener { _, checkedId -> syncPathTextView.isEnabled = checkedId == R.id.rbCustomLocation }

		val libraryId = intent.getIntExtra(serverIdExtra, -1)
		if (libraryId < 0) return

		lazyLibraryProvider.value
			.getLibrary(LibraryId(libraryId))
			.eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response<Library, Unit>({ result ->
				if (result == null) return@response

				library = result

				chkLocalOnly.findView().isChecked = result.isLocalOnly
				chkIsUsingExistingFiles.findView().isChecked = result.isUsingExistingFiles
				chkIsUsingLocalConnectionForSync.findView().isChecked = result.isSyncLocalConnectionsOnly
				chkIsWakeOnLanEnabled.findView().isChecked = result.isWakeOnLanEnabled

				val customSyncPath = result.customSyncedFilesPath
				if (customSyncPath != null && customSyncPath.isNotEmpty()) syncPathTextView.text = customSyncPath

				when (result.syncedFileLocation) {
					SyncedFileLocation.EXTERNAL -> syncFilesRadioGroup.check(R.id.rbPublicLocation)
					SyncedFileLocation.INTERNAL -> syncFilesRadioGroup.check(R.id.rbPrivateToApp)
					SyncedFileLocation.CUSTOM -> syncFilesRadioGroup.check(R.id.rbCustomLocation)
					null -> syncFilesRadioGroup.clearCheck()
				}

				txtAccessCode.findView().setText(result.accessCode)
				txtUserName.findView().setText(result.userName)
				txtPassword.findView().setText(result.password)
			}, this))
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode != permissionsRequestInteger) return

		for (grantResult in grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue
			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show()
			saveButton.findView().isEnabled = true
			return
		}

		saveLibraryAndFinish()
	}

	private fun saveLibraryAndFinish() {
		val library = library ?: return

		lazyLibraryProvider.value.saveLibrary(library).eventually(com.namehillsoftware.projectblue.shared.promises.extensions.LoopedInPromise.response<Library, Unit>({
			saveButton.findView().text = getText(R.string.btn_saved)
			finish()
		}, this))
	}

	companion object {
		@JvmField
		val serverIdExtra = EditClientSettingsActivity::class.java.canonicalName + ".serverIdExtra"
		private const val selectDirectoryResultId = 93
		private const val permissionsRequestInteger = 1
	}
}
