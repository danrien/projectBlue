package com.lasthopesoftware.bluewater.servers.library.access;

import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue;

import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class FilesystemResponseHandler<T extends AbstractIntKeyStringValue> extends DefaultHandler {
	
	private String currentValue;
	private String currentKey;
	
	public List<T> items;
	
	private Class<T> newClass;
	
	public FilesystemResponseHandler(Class<T> c) {
		items = new ArrayList<>();
		newClass = c;
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		
		if (qName.equalsIgnoreCase("item")) {
			currentKey = attributes.getValue("Name");
			
		}
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		if (qName.equalsIgnoreCase("item")) {
			T newItem;
			try {
				newItem = newClass.newInstance();
				newItem.setKey(Integer.parseInt(currentValue));
				newItem.setValue(currentKey);
				items.add(newItem);
			} catch (InstantiationException e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
			} catch (IllegalAccessException e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
			}
		}
	}
}
