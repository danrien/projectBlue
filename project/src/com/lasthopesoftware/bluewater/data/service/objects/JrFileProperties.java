package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class JrFileProperties {
	private int mFileKey;
	private ConcurrentSkipListMap<String, String> mProperties = null;
	private static ExecutorService filePropertiesExecutor = Executors.newSingleThreadExecutor();
	private static ConcurrentHashMap<Integer, ConcurrentSkipListMap<String, String>> mPropertiesCache = new ConcurrentHashMap<Integer, ConcurrentSkipListMap<String,String>>();
	
	public JrFileProperties(int fileKey) {
		
		mFileKey = fileKey;
		
		if (mPropertiesCache.containsKey(mFileKey)) mProperties = mPropertiesCache.get(mFileKey);
		
		if (mProperties == null) {
			mProperties = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER); 
			mPropertiesCache.put(mFileKey, mProperties);
		}
	}
	
	public void setProperty(String name, String value) {
		if (mProperties.containsKey(name) && mProperties.get(name).equals(value)) return;

		AsyncTask<String, Void, Boolean> setPropertyTask = new AsyncTask<String, Void, Boolean>() {
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					JrConnection conn = new JrConnection("File/SetInfo", "File=" + params[0], "Field=" + params[1], "Value=" + params[2]);
					conn.setReadTimeout(5000);
					conn.getInputStream();
					return true;
				} catch (Exception e) {
					return false;
				}
			}
		};
		setPropertyTask.executeOnExecutor(filePropertiesExecutor, String.valueOf(mFileKey), name, value);
		
		mProperties.put(name, value);
	}
	
	public SortedMap<String, String> getProperties() throws IOException {
		if (mProperties.size() == 0)
			return getRefreshredProperties();
		
		return Collections.unmodifiableSortedMap(mProperties);
	}
	
	public SortedMap<String, String> getRefreshredProperties() throws IOException {
		SortedMap<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		SimpleTask<String, Void, SortedMap<String,String>> filePropertiesTask = new SimpleTask<String, Void, SortedMap<String,String>>();
		filePropertiesTask.addOnExecuteListener(new OnExecuteListener<String, Void, SortedMap<String,String>>() {
			
			@Override
			public void onExecute(ISimpleTask<String, Void, SortedMap<String, String>> owner, String... params) throws IOException {
				TreeMap<String, String> returnProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
				
				owner.setResult(returnProperties);
				
				try {
					JrConnection conn = new JrConnection("File/GetInfo", "File=" + String.valueOf(mFileKey));
					conn.setReadTimeout(45000);
					try {
				    	XmlElement xml = Xmlwise.createXml(IOUtils.toString(conn.getInputStream()));
				    	if (xml.size() < 1) return;
				    	
				    	for (XmlElement el : xml.get(0))
				    		returnProperties.put(el.getAttribute("Name"), el.getValue());
					} finally {
						conn.disconnect();
					}
				} catch (MalformedURLException e) {
					LoggerFactory.getLogger(JrFileProperties.class).error(e.toString(), e);
				} catch (XmlParseException e) {
					LoggerFactory.getLogger(JrFileProperties.class).error(e.toString(), e);
				}
			}
		});
		
		filePropertiesTask.addOnErrorListener(new OnErrorListener<String, Void, SortedMap<String,String>>() {
			
			@Override
			public boolean onError(ISimpleTask<String, Void, SortedMap<String, String>> owner, Exception innerException) {
				return !(innerException instanceof IOException);
			}
		});

		try {
			SortedMap<String, String> filePropertiesResult = filePropertiesTask.executeOnExecutor(filePropertiesExecutor).get();
			
			if (filePropertiesTask.getState() == SimpleTaskState.ERROR) {
				for (Exception e : filePropertiesTask.getExceptions()) {
					if (e instanceof IOException) throw (IOException)e;
				}
			}
			
			if (filePropertiesResult == null) return Collections.unmodifiableSortedMap(mProperties);  
			
			result = Collections.unmodifiableSortedMap(filePropertiesResult);
			
			mProperties.putAll(filePropertiesResult);
		} catch (InterruptedException e) {
			LoggerFactory.getLogger(JrFileProperties.class).error(e.toString(), e);
		} catch (ExecutionException e) {
			LoggerFactory.getLogger(JrFileProperties.class).error(e.toString(), e);
		}
		
		return result;
	}
	
	public String getProperty(String name) throws IOException {
		
		if (mProperties.size() == 0 || !mProperties.containsKey(name))
			return getRefreshedProperty(name);
		
		return mProperties.get(name);
	}
	
	public String getRefreshedProperty(String name) throws IOException {
		// Much simpler to just refresh all properties, and shouldn't be very costly (compared to just getting the basic property)
		return getRefreshredProperties().get(name);
	}
	
	/* Utility string constants */
	public static final String ARTIST = "Artist";
	public static final String ALBUM = "Album";
	public static final String DURATION = "Duration";
	public static final String NAME = "Name";
}
