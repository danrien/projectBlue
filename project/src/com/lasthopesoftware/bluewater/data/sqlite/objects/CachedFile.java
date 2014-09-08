package com.lasthopesoftware.bluewater.data.sqlite.objects;

import java.util.Calendar;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "StoredFiles")
public class CachedFile {

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = "libraryId")
	private Library library;
	
	@DatabaseField()
	private Calendar lastAccessedTime;
	
	@DatabaseField(unique = true)
	private String uniqueKey;
	
	@DatabaseField()
	private String fileName;
	
	@DatabaseField()
	private int fileSize;

	/**
	 * @return the library
	 */
	public final Library getLibrary() {
		return library;
	}

	/**
	 * @param library the library to set
	 */
	public final void setLibrary(Library library) {
		this.library = library;
	}

	/**
	 * @return the lastAccessedTime
	 */
	public final Calendar getLastAccessedTime() {
		return lastAccessedTime;
	}

	/**
	 * @param lastAccessedTime the lastAccessedTime to set
	 */
	public final void setLastAccessedTime(Calendar lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	/**
	 * @return the fileName
	 */
	public final String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public final void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the fileSize
	 */
	public final int getFileSize() {
		return fileSize;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public final void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
}
