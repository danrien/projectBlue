package com.lasthopesoftware.bluewater.client.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.permissions.read.ApplicationReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.read.IApplicationReadPermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.write.ApplicationWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.permissions.write.IApplicationWritePermissionsRequirementsProvider;
import com.lasthopesoftware.bluewater.settings.SettingsMenu;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.Lazy;

import java.util.ArrayList;

public class EditClientSettingsActivity extends AppCompatActivity {
	public static final String serverIdExtra = EditClientSettingsActivity.class.getCanonicalName() + ".serverIdExtra";

	private static final int selectDirectoryResultId = 93;
	private static final int permissionsRequestInteger = 1;
	private final LazyViewFinder<Button> saveButton = new LazyViewFinder<>(this, R.id.btnConnect);
	private final LazyViewFinder<EditText> txtAccessCode = new LazyViewFinder<>(this, R.id.txtAccessCode);
	private final LazyViewFinder<EditText> txtUserName = new LazyViewFinder<>(this, R.id.txtUserName);
	private final LazyViewFinder<EditText> txtPassword = new LazyViewFinder<>(this, R.id.txtPassword);
	private final LazyViewFinder<EditText> txtSyncPath = new LazyViewFinder<>(this, R.id.txtSyncPath);
	private final LazyViewFinder<CheckBox> chkLocalOnly = new LazyViewFinder<>(this, R.id.chkLocalOnly);
	private final LazyViewFinder<RadioGroup> rgSyncFileOptions = new LazyViewFinder<>(this, R.id.rgSyncFileOptions);
	private final LazyViewFinder<CheckBox> chkIsUsingExistingFiles = new LazyViewFinder<>(this, R.id.chkIsUsingExistingFiles);
	private final LazyViewFinder<CheckBox> chkIsUsingLocalConnectionForSync = new LazyViewFinder<>(this, R.id.chkIsUsingLocalConnectionForSync);
	private final Lazy<IApplicationWritePermissionsRequirementsProvider> applicationWritePermissionsRequirementsProviderLazy = new Lazy<>(() -> new ApplicationWritePermissionsRequirementsProvider(this));
	private final Lazy<IApplicationReadPermissionsRequirementsProvider> applicationReadPermissionsRequirementsProviderLazy = new Lazy<>(() -> new ApplicationReadPermissionsRequirementsProvider(this));
	private final Lazy<LibraryRepository> lazyLibraryProvider = new Lazy<>(() -> new LibraryRepository(EditClientSettingsActivity.this));
	private final SettingsMenu settingsMenu = new SettingsMenu(this, new AboutTitleBuilder(this));
	private Library library;

	private final OnClickListener connectionButtonListener = v -> {
		saveButton.findView().setEnabled(false);

		if (library == null) {
			library = new Library();
			library.setNowPlayingId(-1);
		}

		library.setAccessCode(txtAccessCode.findView().getText().toString());

		library.setUserName(txtUserName.findView().getText().toString());
		library.setPassword(txtPassword.findView().getText().toString());

		library.setLocalOnly(chkLocalOnly.findView().isChecked());
		library.setCustomSyncedFilesPath(txtSyncPath.findView().getText().toString());
		switch (rgSyncFileOptions.findView().getCheckedRadioButtonId()) {
			case R.id.rbPublicLocation:
				library.setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL);
				break;
			case R.id.rbPrivateToApp:
				library.setSyncedFileLocation(Library.SyncedFileLocation.INTERNAL);
				break;
			case R.id.rbCustomLocation:
				library.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM);
				break;
		}

		library.setIsUsingExistingFiles(chkIsUsingExistingFiles.findView().isChecked());
		library.setIsSyncLocalConnectionsOnly(chkIsUsingLocalConnectionForSync.findView().isChecked());

		final ArrayList<String> permissionsToRequest = new ArrayList<>(2);

		if (applicationReadPermissionsRequirementsProviderLazy.getObject().isReadPermissionsRequiredForLibrary(library))
			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);

		if (applicationWritePermissionsRequirementsProviderLazy.getObject().isWritePermissionsRequiredForLibrary(library))
			permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (permissionsToRequest.size() > 0) {
			final String[] permissionsToRequestArray = permissionsToRequest.toArray(new String[0]);
			ActivityCompat.requestPermissions(EditClientSettingsActivity.this, permissionsToRequestArray, permissionsRequestInteger);

			return;
		}

		saveLibraryAndFinish();
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_server_settings);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		saveButton.findView().setOnClickListener(connectionButtonListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return settingsMenu.buildSettingsMenu(menu);
	}

	@Override
	protected void onStart() {
		super.onStart();

		initializeLibrary(getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		initializeLibrary(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != selectDirectoryResultId) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}

		final String uri = data.getDataString();
		txtSyncPath.findView().setText(uri);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return settingsMenu.handleSettingsMenuClicks(item);
	}

	private void initializeLibrary(Intent intent) {
		final java.io.File externalFilesDir = Environment.getExternalStorageDirectory();
		final TextView syncPathTextView = txtSyncPath.findView();
		if (externalFilesDir != null)
			syncPathTextView.setText(externalFilesDir.getPath());

		final RadioGroup syncFilesRadioGroup = rgSyncFileOptions.findView();
		syncFilesRadioGroup.check(R.id.rbPrivateToApp);

		syncFilesRadioGroup.setOnCheckedChangeListener((group, checkedId) -> syncPathTextView.setEnabled(checkedId == R.id.rbCustomLocation));

		final int libraryId = intent.getIntExtra(serverIdExtra, -1);
		if (libraryId < 0) return;

		lazyLibraryProvider.getObject()
			.getLibrary(new LibraryId(libraryId))
			.eventually(LoopedInPromise.response(new VoidResponse<>(result -> {
				if (result == null) return;

				library = result;

				chkLocalOnly.findView().setChecked(library.isLocalOnly());
				chkIsUsingExistingFiles.findView().setChecked(library.isUsingExistingFiles());
				chkIsUsingLocalConnectionForSync.findView().setChecked(library.isSyncLocalConnectionsOnly());

				final String customSyncPath = library.getCustomSyncedFilesPath();
				if (customSyncPath != null && !customSyncPath.isEmpty())
					syncPathTextView.setText(customSyncPath);

				switch (library.getSyncedFileLocation()) {
					case EXTERNAL:
						syncFilesRadioGroup.check(R.id.rbPublicLocation);
						break;
					case INTERNAL:
						syncFilesRadioGroup.check(R.id.rbPrivateToApp);
						break;
					case CUSTOM:
						syncFilesRadioGroup.check(R.id.rbCustomLocation);
						break;
				}

				txtAccessCode.findView().setText(library.getAccessCode());

				txtUserName.findView().setText(library.getUserName());
				txtPassword.findView().setText(library.getPassword());
			}), this));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode != permissionsRequestInteger) return;

		for (int grantResult : grantResults) {
			if (grantResult == PackageManager.PERMISSION_GRANTED) continue;

			Toast.makeText(this, R.string.permissions_must_be_granted_for_settings, Toast.LENGTH_LONG).show();
			saveButton.findView().setEnabled(true);
			return;
		}

		saveLibraryAndFinish();
	}

	private void saveLibraryAndFinish() {
		lazyLibraryProvider.getObject().saveLibrary(library).eventually(LoopedInPromise.response(result -> {
			saveButton.findView().setText(getText(R.string.btn_saved));
			finish();
			return null;
		}, this));
	}
}
