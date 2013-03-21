package jrAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import jrFileSystem.JrFile;
import jrFileSystem.JrPlaylist;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class JrPlaylistXmlHandler extends DefaultHandler {
	
	private HashMap<String, JrPlaylist> playlists = new HashMap<String, JrPlaylist>();
	private JrPlaylist currentPlaylist;
	private String currentValue;
	private String currentKey;
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		currentValue = "";

		if (qName.equalsIgnoreCase("item"))
			currentPlaylist = new JrPlaylist();
		
		if (qName.equalsIgnoreCase("field"))
			currentKey = attributes.getValue("Name");
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch,start,length);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("field")) {
			if (currentKey.equalsIgnoreCase("id"))
				currentPlaylist.setKey(Integer.parseInt(currentValue));
			
			if (currentKey.equalsIgnoreCase("group"))
				currentPlaylist.setGroup(currentValue);
			
			if (currentKey.equalsIgnoreCase("name"))
				currentPlaylist.setValue(currentValue);
			
			if (currentKey.equalsIgnoreCase("path")) {
				currentPlaylist.setPath(currentValue);
				playlists.put(currentValue, currentPlaylist);
				
				// Add existing children
				for (String key : playlists.keySet()) {
					int lastKeyPathIndex = key.lastIndexOf('\\');
					if (lastKeyPathIndex > -1 && key.indexOf(currentValue) == 0 && lastKeyPathIndex == key.length()) currentPlaylist.addPlaylist(playlists.get(key));
				}
				
				// Add to existing parent if it has a path
				int lastPathIndex = currentValue.lastIndexOf('\\');
				if (lastPathIndex > -1) {
					String parent = currentValue.substring(0, lastPathIndex - 1);
					if (playlists.containsKey(parent)) playlists.get(parent).addPlaylist(currentPlaylist);
				}
			}
		}
			
	}
	
	/**
	 * @return the response
	 */
	public ArrayList<JrPlaylist> getPlaylists() {
		ArrayList<JrPlaylist> returnList = new ArrayList<JrPlaylist>(playlists.size());
		for (JrPlaylist playlist : playlists.values()) {
			if (playlist.getSubItems().isEmpty()) returnList.add(playlist);
		}
		
		return returnList;
	}
}
