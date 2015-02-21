package com.lasthopesoftware.bluewater.servers.library.access;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.lasthopesoftware.bluewater.servers.library.items.Item;

public class FilesystemResponse {

	public static List<Item> GetItems(InputStream is) {

		try {
			
			final SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    	final FilesystemResponseHandler<Item> jrResponseHandler = new FilesystemResponseHandler<Item>(Item.class);
	    	sp.parse(is, jrResponseHandler);
	    	
	    	return jrResponseHandler.items;
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (SAXException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		} catch (ParserConfigurationException e) {
			LoggerFactory.getLogger(FilesystemResponse.class).error(e.toString(), e);
		}
		
		return new ArrayList<Item>();
	}

}
