package com.namehillsoftware.projectblue.shared;

import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class StandardRequest {

	private final boolean status;
	public final HashMap<String, String> items = new HashMap<>();

	StandardRequest(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}

	public static StandardRequest fromInputStream(InputStream is) {
		try {
			final SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    	final StandardResponseHandler jrResponseHandler = new StandardResponseHandler();
			sp.parse(is, jrResponseHandler);
			return jrResponseHandler.getResponse();
		} catch (Exception e) {
			LoggerFactory.getLogger(StandardRequest.class).error(e.toString(), e);
		}

    	return null;
	}
}
