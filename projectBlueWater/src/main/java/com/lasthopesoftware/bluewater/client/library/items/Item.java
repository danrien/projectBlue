package com.lasthopesoftware.bluewater.client.library.items;

import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.IFileListParameterProvider;
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;



public class Item extends AbstractIntKeyStringValue implements IItem, IFileListParameterProvider {

	public Item(int key, String value) {
		super(key, value);

	}
	
	public Item(int key) {
		super();

		this.setKey(key);
	}
	
	public Item() {
		super();

	}

	@Override
    public int hashCode() {
        return getKey();
    }

	@Override
	public String[] getFileListParameters() {
		return new String[] {"Browse/Files", "ID=" + String.valueOf(getKey())};
	}
}
