package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.*

internal class ItemResponseHandler : DefaultHandler() {
	private var currentValue: String? = null
	private var currentKey: String? = null
	private var currentPlaylistId: String? = null

	val items: MutableList<Item> = ArrayList()

	override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
		currentValue = ""
		currentKey = ""
		if (!"item".equals(qName, ignoreCase = true)) return

		currentKey = attributes.getValue("Name")
		currentPlaylistId = attributes.getValue("PlaylistID")
	}

	override fun characters(ch: CharArray, start: Int, length: Int) {
		currentValue = String(ch, start, length)
	}

	override fun endElement(uri: String, localName: String, qName: String) {
		if (!"item".equals(qName, ignoreCase = true)) return

		val currentVal = currentValue?.toInt() ?: return
		val item = Item(currentVal, currentKey)

		val playlistIdString = currentPlaylistId
		if (!playlistIdString.isNullOrEmpty()) item.playlistId = playlistIdString.toInt()

		items.add(item)
	}
}
