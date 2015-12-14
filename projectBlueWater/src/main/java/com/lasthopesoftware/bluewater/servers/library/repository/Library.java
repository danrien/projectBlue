package com.lasthopesoftware.bluewater.servers.library.repository;

import android.content.Context;
import android.os.Environment;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.access.stringlist.FileStringListUtilities;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Library {

	public static final String tableName = "LIBRARIES";
	public static final String libraryNameColumn = "libraryName";
	public static final String accessCodeColumn = "accessCode";
	public static final String authKeyColumn = "authKey";
	public static final String isLocalOnlyColumn = "isLocalOnly";

	private int id;
	
	// Remote connection fields
	private String libraryName;
	private String accessCode;
	private String authKey;
	private boolean isLocalOnly = false;
	private boolean isRepeating = false;
	private int nowPlayingId = -1;
	private int nowPlayingProgress = -1;
	private ViewType selectedViewType;
	private int selectedView = -1;
	private String savedTracksString;
	private String customSyncedFilesPath;
	private SyncedFileLocation syncedFileLocation;
	private boolean isUsingExistingFiles;
	private boolean isSyncLocalConnectionsOnly;

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
		savedTracksString = FileStringListUtilities.serializeFileStringList(files);
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

	public boolean isSyncLocalConnectionsOnly() {
		return isSyncLocalConnectionsOnly;
	}

	public void setIsSyncLocalConnectionsOnly(boolean isSyncLocalConnections) {
		this.isSyncLocalConnectionsOnly = isSyncLocalConnections;
	}

	public ViewType getSelectedViewType() {
		return selectedViewType;
	}

	public void setSelectedViewType(ViewType selectedViewType) {
		this.selectedViewType = selectedViewType;
	}

	public void setId(int id) {
		this.id = id;
	}

	public enum SyncedFileLocation {
		EXTERNAL,
		INTERNAL,
		CUSTOM
	}

	public enum ViewType {
		StandardServerView,
		PlaylistView,
		DownloadView
	}

	public static final HashSet<ViewType> serverViewTypes = new HashSet<>(Arrays.asList(Library.ViewType.StandardServerView, Library.ViewType.PlaylistView));
}
