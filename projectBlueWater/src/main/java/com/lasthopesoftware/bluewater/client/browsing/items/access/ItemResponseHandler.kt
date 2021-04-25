package com.lasthopesoftware.bluewater.client.browsing.items.access;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

class ItemResponseHandler extends DefaultHandler {

	private String currentValue;
	private String currentKey;
	private String currentPlaylistId;
	
	public final List<Item> items = new ArrayList<>();

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		currentValue = "";
		currentKey = "";
		
		if (!"item".equalsIgnoreCase(qName)) return;

		currentKey = attributes.getValue("Name");
		currentPlaylistId = attributes.getValue("PlaylistID");
	}
	
	public void characters(char[] ch, int start, int length) {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) {
		if (!"item".equalsIgnoreCase(qName)) return;

		final Item item = new Item(Integer.parseInt(currentValue), currentKey);

		if (currentPlaylistId != null && !currentPlaylistId.isEmpty())
			item.setPlaylistId(Integer.parseInt(currentPlaylistId));

		items.add(item);
	}
}
