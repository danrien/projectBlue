package com.lasthopesoftware.bluewater.servers.repository;

import android.content.Context;
import android.os.Environment;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@DatabaseTable(tableName = "LIBRARIES")
public class Library {
	
	@DatabaseField(generatedId = true)
	private int id;
	
	// Remote connection fields
	@DatabaseField(canBeNull = false, columnDefinition = "VARCHAR(50)")
	private String libraryName;
	@DatabaseField(canBeNull = false, columnDefinition = "VARCHAR(30)")
	private String accessCode;
	@DatabaseField(columnDefinition = "VARCHAR(100)")
	private String authKey;
	@DatabaseField
	private boolean isLocalOnly = false;
	@DatabaseField
	private boolean isRepeating = false;
	
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int nowPlayingId;
	
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int nowPlayingProgress;
	
	@DatabaseField(defaultValue = "-1", canBeNull = false)
	private int selectedView = -1;
	
	@DatabaseField
	private String savedTracksString;

	@DatabaseField
	private String customSyncedFilesPath;

	@DatabaseField
	private SyncedFileLocation syncedFileLocation;

	@DatabaseField
	private boolean isUsingExistingFiles;
	
	@ForeignCollectionField(eager = true)
	private Collection<StoredFile> storedFiles = null;
	
	@ForeignCollectionField(eager = true)
	private Collection<StoredItem> storedItems = null;
	
	@ForeignCollectionField()
	private Collection<CachedFile> cachedFiles = null;
	
	/**
	 * @return the nowPlayingId
	 */
	public int getNowPlayingId() {
		return nowPlayingId;
	}
	/**
	 * @param nowPlayingId the nowPlayingId to set
	 */
	public void setNowPlayingId(int nowPlayingId) {
		this.nowPlayingId = nowPlayingId;
	}
	
	/**
	 * @return the mLibraryName
	 */
	public String getLibraryName() {
		return libraryName;
	}
	/**
	 * @param libraryName the mLibraryName to set
	 */
	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}
	/**
	 * @return the mAccessCode
	 */
	public String getAccessCode() {
		return accessCode;
	}
	/**
	 * @param accessCode the mAccessCode to set
	 */
	public void setAccessCode(String accessCode) {
		this.accessCode = accessCode;
	}

	/**
	 * @return the authKey
	 */
	public String getAuthKey() {
		return authKey;
	}
	/**
	 * @param authKey the authKey to set
	 */
	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	/**
	 * @return the nowPlayingProgress
	 */
	public int getNowPlayingProgress() {
		return nowPlayingProgress;
	}
	/**
	 * @param nowPlayingProgress the nowPlayingProgress to set
	 */
	public void setNowPlayingProgress(int nowPlayingProgress) {
		this.nowPlayingProgress = nowPlayingProgress;
	}
		
	public String getSavedTracksString() {
		return savedTracksString;
	}
	
	public void setSavedTracksString(String savedTracksString) {
		this.savedTracksString = savedTracksString;
	}
		
	public void setSavedTracks(List<IFile> files) {
		savedTracksString = Files.serializeFileStringList(files);
	}
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @return the isLocalOnly
	 */
	public boolean isLocalOnly() {
		return isLocalOnly;
	}
	/**
	 * @param isLocalOnly the isLocalOnly to set
	 */
	public void setLocalOnly(boolean isLocalOnly) {
		this.isLocalOnly = isLocalOnly;
	}
	/**
	 * @return the selectedView
	 */
	public int getSelectedView() {
		return selectedView;
	}
	/**
	 * @param selectedView the selectedView to set
	 */
	public void setSelectedView(int selectedView) {
		this.selectedView = selectedView;
	}
	/**
	 * @return the isRepeating
	 */
	public boolean isRepeating() {
		return isRepeating;
	}
	/**
	 * @param isRepeating the isRepeating to set
	 */
	public void setRepeating(boolean isRepeating) {
		this.isRepeating = isRepeating;
	}
	
	public Collection<StoredFile> getStoredFiles() {
		if (storedFiles == null)
			storedFiles = new ArrayList<>();
		
		return storedFiles;
	}
	
	public Collection<StoredItem> getStoredItems() {
		if (storedItems == null)
			storedItems = new ArrayList<>();
		
		return storedItems;
	}
	/**
	 * @return the cachedFiles
	 */
	public final Collection<CachedFile> getCachedFiles() {
		if (cachedFiles == null)
			cachedFiles = new ArrayList<>();
		
		return cachedFiles;
	}

	public String getCustomSyncedFilesPath() {
		return customSyncedFilesPath;
	}

	public void setCustomSyncedFilesPath(String customSyncedFilesPath) {
		this.customSyncedFilesPath = customSyncedFilesPath;
	}

	public File getSyncDir(Context context) {
		return syncedFileLocation != SyncedFileLocation.CUSTOM ? buildSyncDir(context, syncedFileLocation) : new File(customSyncedFilesPath);
	}

	private static File buildSyncDir(Context context, SyncedFileLocation syncedFileLocation) {
		File parentSyncDir = null;
		switch (syncedFileLocation) {
			case EXTERNAL:
				parentSyncDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
				break;
			case INTERNAL:
				parentSyncDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
				break;
		}

		return parentSyncDir;
	}

	public SyncedFileLocation getSyncedFileLocation() {
		return syncedFileLocation;
	}

	public void setSyncedFileLocation(SyncedFileLocation syncedFileLocation) {
		this.syncedFileLocation = syncedFileLocation;
	}

	public boolean isUsingExistingFiles() {
		return isUsingExistingFiles;
	}

	public void setIsUsingExistingFiles(boolean isUsingExistingFiles) {
		this.isUsingExistingFiles = isUsingExistingFiles;
	}

	public enum SyncedFileLocation {
		EXTERNAL,
		INTERNAL,
		CUSTOM
	}
}
