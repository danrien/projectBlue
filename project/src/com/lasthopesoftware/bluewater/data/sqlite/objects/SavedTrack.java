package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SAVED_TRACKS")
public class SavedTrack {
	
	@DatabaseField(generatedId = true)
	private int id;
	@DatabaseField(columnName = "TRACK_ID")
	private int trackId;
	@DatabaseField(foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
	private Library library;

	/**
	 * @return the trackId
	 */
	public int getTrackId() {
		return trackId;
	}

	/**
	 * @param trackId the trackId to set
	 */
	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

	/**
	 * @return the library
	 */
	public Library getLibrary() {
		return library;
	}

	/**
	 * @param library the library to set
	 */
	public void setLibrary(Library library) {
		this.library = library;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
}
