package com.lasthopesoftware.bluewater.data.service.access;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.slf4j.LoggerFactory;

import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.data.service.access.connection.JrConnection;

public class JrByteResponse extends AsyncTask<String, Void, byte[]> {

	@Override
	protected byte[] doInBackground(String... params) {
		InputStream is = null;
		// Add base url
	
		try {
			JrConnection conn = new JrConnection(params);
			try {
				is = conn.getInputStream();
				
				int nRead = 0;
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] data = new byte[16384];
				
				while ((nRead = is.read(data, 0, data.length)) != -1)
					buffer.write(data, 0, nRead);
				
				return buffer.toByteArray();
			} finally {
				conn.disconnect();
			}
		} catch (MalformedURLException e) {
			LoggerFactory.getLogger(JrByteResponse.class).error(e.toString(), e);
		} catch (IOException e) {
			LoggerFactory.getLogger(JrByteResponse.class).error(e.toString(), e);
		}
		
		return new byte[0];
	}

}
