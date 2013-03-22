package jrAccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrFileSystem.JrPlaylist;

import org.xml.sax.SAXException;

import android.os.AsyncTask;

public class JrPlaylistResponse extends AsyncTask<String, Void, ArrayList<JrPlaylist>> {

	@Override
	protected ArrayList<JrPlaylist> doInBackground(String... params) {
		ArrayList<JrPlaylist> returnFiles = new ArrayList<JrPlaylist>();
		
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrPlaylistXmlHandler jrPlaylistXml = new JrPlaylistXmlHandler();
	    	sp.parse(conn.getInputStream(), jrPlaylistXml);
	    	
	    	returnFiles = jrPlaylistXml.getPlaylists();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFiles;
	}

}
