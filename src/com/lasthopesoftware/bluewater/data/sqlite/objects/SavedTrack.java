package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SAVED_TRACKS")
public class SavedTrack {
	
	@DatabaseField(generatedId = true)
	private Integer id;
	@DatabaseField(columnName = "TRACK_ID")
	private Integer trackId;
	@DatabaseField(foreign = true, foreignAutoCreate = true)
	private Library library;

	/**
	 * @return the trackId
	 */
	public Integer getTrackId() {
		return trackId;
	}

	/**
	 * @param trackId the trackId to set
	 */
	public void setTrackId(Integer trackId) {
		this.trackId = trackId;
	}
}
