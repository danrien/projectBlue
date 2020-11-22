package com.namehillsoftware.projectblue.shared;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;

import com.lasthopesoftware.bluewater.client.connection.ConnectionInfo;

/**
 * Created by david on 7/24/15.
 */
public class IoCommon {
	public static final String FileUriScheme = "file";
	public static final String httpUriScheme = "http";
	public static final String httpsUriScheme = "https";

	public static boolean isWifiConnected(Context context) {
		return ConnectionInfo.getConnectionType(context) == ConnectivityManager.TYPE_WIFI;
	}

	public static boolean isPowerConnected(Context context) {
		final Intent batteryStatusReceiver = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (batteryStatusReceiver == null) return false;

		final int pluggedStatus = batteryStatusReceiver.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return pluggedStatus > 0;
	}
}
