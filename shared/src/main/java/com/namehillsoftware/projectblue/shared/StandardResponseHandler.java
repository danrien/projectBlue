package com.namehillsoftware.projectblue.shared;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class StandardResponseHandler extends DefaultHandler {

	private StandardRequest response;
	private String currentValue;
	private String currentKey;

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";
		currentKey = "";
		if (qName.equalsIgnoreCase("response"))
			response = new StandardRequest(attributes.getValue("Status"));

		if (qName.equalsIgnoreCase("item")) {
			currentKey = attributes.getValue("Name");

		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("item"))
			response.items.put(currentKey, currentValue);
	}

	/**
	 * @return the response
	 */
	public StandardRequest getResponse() {
		return response;
	}
}
