package com.lasthopesoftware.bluewater.client.library.items.media.files;

import android.support.annotation.NonNull;

public class File implements IFile {

	private int key;

	public File(int key) {
		this.setKey(key);
	}
	
	@Override
	public int getKey() {
		return key;
	}

	@Override
	public void setKey(int key) {
		this.key = key;
	}

	@Override
	public int compareTo(@NonNull IFile another) {
		return getKey() - another.getKey();
	}

	@Override
	public int hashCode() {
		return key;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof IFile ? compareTo((IFile)obj) == 0 : super.equals(obj);
	}
}
