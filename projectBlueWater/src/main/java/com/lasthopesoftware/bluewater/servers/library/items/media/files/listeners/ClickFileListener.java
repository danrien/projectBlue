package com.lasthopesoftware.bluewater.servers.library.items.media.files.listeners;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.lasthopesoftware.bluewater.servers.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.servers.connection.helpers.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IItemFiles;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.PlaybackService;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.IDataTask.OnErrorListener;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

import java.io.IOException;

public class ClickFileListener implements OnItemClickListener {

	private final IItemFiles mItemFiles;
	
	public ClickFileListener(IItemFiles itemFiles) {
		mItemFiles = itemFiles;
	}
	
	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		mItemFiles.getFileStringList(new OnCompleteListener<String>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, String> owner, String result) {
				if (owner.getState() == SimpleTaskState.ERROR) return;
				
				PlaybackService.launchMusicService(view.getContext(), position, result);
			}
		}, new OnErrorListener<String>() {

			@Override
			public boolean onError(ISimpleTask<String, Void, String> owner, boolean isHandled, Exception innerException) {
				if (!(innerException instanceof IOException)) return false;
				
				PollConnection.Instance.get(view.getContext()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
					
					@Override
					public void onConnectionRegained() {
						onItemClick(parent, view, position, id);
					}
				});
				
				WaitForConnectionDialog.show(view.getContext());
				return true;
			}
		});
	}
}
