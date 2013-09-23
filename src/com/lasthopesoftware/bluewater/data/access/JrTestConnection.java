package com.lasthopesoftware.bluewater.data.access;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class JrTestConnection implements Callable<Boolean> {
	
	@Override
	public Boolean call() throws Exception {
		Boolean result = Boolean.FALSE;
		
		try {
			JrConnection conn = new JrConnection("Alive");
	    	
			JrResponse responseDao = JrResponse.fromInputStream(conn.getInputStream());
	    	
	    	result = responseDao != null && responseDao.isStatus() ? Boolean.TRUE : Boolean.FALSE;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean doTest() throws InterruptedException, ExecutionException {
		FutureTask<Boolean> statusTask = new FutureTask<Boolean>(new JrTestConnection());
		Thread statusThread = new Thread(statusTask);
		statusThread.setName("Checking connection status");
		statusThread.setPriority(Thread.MIN_PRIORITY);
		statusThread.start();
		return statusTask.get().booleanValue();
	}
}
