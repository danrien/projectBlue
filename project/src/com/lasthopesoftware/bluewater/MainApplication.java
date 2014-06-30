package com.lasthopesoftware.bluewater;

import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.StrictMode;

import com.lasthopesoftware.bluewater.exceptions.LoggerUncaughtExceptionHandler;

public class MainApplication extends Application {
	
	public static boolean DEBUG_MODE = false;
	
	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(new LoggerUncaughtExceptionHandler());
		LoggerFactory.getLogger(MainApplication.class).info("Uncaught exceptions logging to custom uncaught exception handler."); 
		
		try {
			DEBUG_MODE = (ApplicationInfo.FLAG_DEBUGGABLE & getPackageManager().getPackageInfo(getPackageName(), 0).applicationInfo.flags) != 0;
		} catch (NameNotFoundException e) {
		    LoggerFactory.getLogger(getClass()).error(e.getMessage(), e);
		}
		
		if (DEBUG_MODE) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectDiskReads()
	                 .detectDiskWrites()
	                 .detectNetwork()   // or .detectAll() for all detectable problems
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectLeakedSqlLiteObjects()
	                 .detectLeakedClosableObjects()
	                 .penaltyLog()
//	                 .penaltyDeath()
	                 .build());
	     }
	}
}
