package com.lasthopesoftware.bluewater.data.access.connection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.lasthopesoftware.bluewater.data.access.JrResponse;

public class JrTestConnection implements Callable<Boolean> {
	
	private static int stdTimeoutTime = 30000;
	private int mTimeout;
	
	public JrTestConnection() {
		mTimeout = stdTimeoutTime;
	}
	
	public JrTestConnection(int timeout) {
		mTimeout = timeout;
	}
	
	@Override
	public Boolean call() throws Exception {
		Boolean result = Boolean.FALSE;
		
		try {
			JrConnection conn = new JrConnection("Alive");
	    	conn.setConnectTimeout(mTimeout);
			JrResponse responseDao = JrResponse.fromInputStream(conn.getInputStream());
	    	
	    	result = responseDao != null && responseDao.isStatus() ? Boolean.TRUE : Boolean.FALSE;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean doTest(int timeout) {
		return doTest(new JrTestConnection(timeout));
	}
	
	public static boolean doTest() {
		return doTest(new JrTestConnection());
	}
	
	private static boolean doTest(JrTestConnection testConnection) {
		try {
			FutureTask<Boolean> statusTask = new FutureTask<Boolean>(testConnection);
			Thread statusThread = new Thread(statusTask);
			statusThread.setName("Checking connection status");
			statusThread.setPriority(Thread.MIN_PRIORITY);
			statusThread.start();
			return statusTask.get().booleanValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
