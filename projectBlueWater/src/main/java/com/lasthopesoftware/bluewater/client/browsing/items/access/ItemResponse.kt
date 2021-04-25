package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

object ItemResponse {
	@JvmStatic
	fun parseItems(inputStream: InputStream): List<Item> {
		try {
			val sp = SAXParserFactory.newInstance().newSAXParser()
			val jrResponseHandler = ItemResponseHandler()
			sp.parse(inputStream, jrResponseHandler)
			return jrResponseHandler.items
		} catch (e: IOException) {
			LoggerFactory.getLogger(ItemResponse::class.java).error(e.toString(), e)
		} catch (e: ParserConfigurationException) {
			LoggerFactory.getLogger(ItemResponse::class.java).error(e.toString(), e)
		} catch (e: SAXException) {
			LoggerFactory.getLogger(ItemResponse::class.java).error(e.toString(), e)
		}
		return ArrayList()
	}
}
