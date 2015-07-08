package com.lasthopesoftware.bluewater.disk.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "StoredFiles")
public class StoredFile {

	public static final String downloadIdName = "downloadId";
	public static final String serviceIdColumnName = "serviceId";
	public static final String libraryIdColumnName = "libraryId";
	public static final String pathColumnName = "path";

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = "libraryId", uniqueCombo = true)
	private Library library;
	
	@DatabaseField
	private int storedMediaId;

	@DatabaseField(uniqueCombo = true)
	private int serviceId;

	@DatabaseField(unique = true)
	private long downloadId = -1;

	@DatabaseField
	private boolean isDownloadComplete;

	@DatabaseField(unique = true)
	private String path;

	@DatabaseField
	private boolean isOwner;

	public int getId() {
		return id;
	}

	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}

	public int getStoredMediaId() {
		return storedMediaId;
	}

	public void setStoredMediaId(int storedMediaId) {
		this.storedMediaId = storedMediaId;
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	public long getDownloadId() {
		return downloadId;
	}

	public void setDownloadId(long downloadId) {
		this.downloadId = downloadId;
	}

	public boolean isDownloadComplete() {
		return isDownloadComplete;
	}

	public void setIsDownloadComplete(boolean isDownloadComplete) {
		this.isDownloadComplete = isDownloadComplete;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isOwner() {
		return isOwner;
	}

	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}
}