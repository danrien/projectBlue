package com.lasthopesoftware.bluewater.client.stored.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.slf4j.LoggerFactory;

public class SyncAlarmBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		LoggerFactory.getLogger(getClass()).info("Received alarm to begin sync.");
		StoredSyncService.doSync(context);
	}
}
