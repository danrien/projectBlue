package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "StoredFiles")
public class StoredFile {

	@DatabaseField(generatedId = true)
	private int id;
	
	@DatabaseField(foreign = true, columnName = "libraryId")
	private Library library;
	
	@DatabaseField
	private int storedMediaId;
	
	@DatabaseField
	private int serviceId;

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
}