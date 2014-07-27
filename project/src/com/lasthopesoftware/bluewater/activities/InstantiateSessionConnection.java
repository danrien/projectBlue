package com.lasthopesoftware.bluewater.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection.BuildingSessionConnectionStatus;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.BuildSessionConnection.OnBuildSessionStateChangeListener;

public class InstantiateSessionConnection extends Activity {
	
	private static final String START_ACTIVITY_FOR_RETURN = "com.lasthopesoftware.bluewater.activities.InstantiateSessionConnection.START_ACTIVITY_FOR_RETURN";
	
	private static final int ACTIVITY_LAUNCH_DELAY = 1500;
	
	private final TextView lblConnectionStatus = (TextView)findViewById(R.id.lblConnectionStatus);		
	private final Intent selectServerIntent = new Intent(this, SelectServer.class);
	private final Intent browseLibraryIntent = new Intent(this, BrowseLibrary.class);
	
	public static void startForReturn(Context context) {
		Intent intent = new Intent(context, InstantiateSessionConnection.class);
		intent.setAction(START_ACTIVITY_FOR_RETURN);
		context.startActivity(intent);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_status);
		
		handleBuildStatusChange(BuildSessionConnection.build(this, new OnBuildSessionStateChangeListener() {
			
			@Override
			public void onBuildSessionStatusChange(BuildingSessionConnectionStatus status) {
				handleBuildStatusChange(status);
			}
		}));
	}
		
	private void handleBuildStatusChange(BuildingSessionConnectionStatus status) {
		switch (status) {
		case GETTING_LIBRARY:
			lblConnectionStatus.setText(R.string.lbl_getting_library_details);
			return;
		case GETTING_LIBRARY_FAILED:
			lblConnectionStatus.setText(R.string.lbl_please_connect_to_valid_server);
			launchActivityDelayed(selectServerIntent);
			return;
		case BUILDING_CONNECTION:
			lblConnectionStatus.setText(R.string.lbl_connecting_to_server_library);
			return;
		case BUILDING_CONNECTION_FAILED:
			lblConnectionStatus.setText(R.string.lbl_error_connecting_try_again);
			launchActivityDelayed(selectServerIntent);
			return;
		case GETTING_VIEW:
			lblConnectionStatus.setText(R.string.lbl_getting_library_views);
			return;
		case GETTING_VIEW_FAILED:
			lblConnectionStatus.setText(R.string.lbl_library_no_views);
			launchActivityDelayed(selectServerIntent);
			return;
		case BUILDING_SESSION_COMPLETE:
			lblConnectionStatus.setText(R.string.lbl_connected);
			if (!getIntent().getAction().equals(START_ACTIVITY_FOR_RETURN))
				launchActivityDelayed(browseLibraryIntent);
			finish();
			return;
		}
	}
	
	private void launchActivityDelayed(Intent intent) {
		final Handler handler = new Handler();
		handler.postDelayed(new LaunchRunnable(this, intent), ACTIVITY_LAUNCH_DELAY);
	}
	
	private static class LaunchRunnable implements Runnable {
		private final Intent mIntent;
		private final Context mContext;
		
		public LaunchRunnable(final Context context, final Intent intent) {
			mIntent = intent;
			mContext = context;
		}
		
		@Override
		public void run() {
			mContext.startActivity(mIntent);
		}
		
	}
}
