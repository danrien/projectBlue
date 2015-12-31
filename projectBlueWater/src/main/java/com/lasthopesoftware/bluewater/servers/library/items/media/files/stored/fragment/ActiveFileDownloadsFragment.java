package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.fragment;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.servers.connection.SessionConnection;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.StoredFileAccess;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.fragment.adapter.ActiveFileDownloadsAdapter;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.bluewater.servers.library.repository.LibrarySession;
import com.lasthopesoftware.bluewater.sync.service.SyncService;
import com.vedsoft.fluent.FluentTask;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

import java.util.List;

/**
 * Created by david on 6/6/15.
 */
public class ActiveFileDownloadsFragment extends Fragment {

    private BroadcastReceiver onFileQueuedReceiver;
    private BroadcastReceiver onFileDownloadedReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final RelativeLayout viewFileslayout = (RelativeLayout) inflater.inflate(R.layout.layout_downloads, container, false);

		final ProgressBar progressBar = (ProgressBar) viewFileslayout.findViewById(R.id.pbLoadingItems);
		final ListView listView = (ListView) viewFileslayout.findViewById(R.id.lvItems);

		listView.setVisibility(View.INVISIBLE);
		progressBar.setVisibility(View.VISIBLE);

		LibrarySession.GetActiveLibrary(getActivity(), new TwoParameterRunnable<FluentTask<Integer,Void,Library>, Library>() {
			@Override
			public void run(FluentTask<Integer, Void, Library> owner, Library library) {
				final StoredFileAccess storedFileAccess = new StoredFileAccess(getActivity(), library);
				storedFileAccess.getDownloadingStoredFiles(new TwoParameterRunnable<FluentTask<Void,Void,List<StoredFile>>, List<StoredFile>>() {
					@Override
					public void run(FluentTask<Void, Void, List<StoredFile>> owner, final List<StoredFile> storedFiles) {
						final ActiveFileDownloadsAdapter activeFileDownloadsAdapter = new ActiveFileDownloadsAdapter(getActivity(), SessionConnection.getSessionConnectionProvider(), storedFiles);

						if (onFileDownloadedReceiver != null)
							localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);

						onFileDownloadedReceiver = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								final int storedFileId = intent.getIntExtra(SyncService.storedFileEventKey, -1);

								for (StoredFile storedFile : storedFiles) {
									if (storedFile.getId() != storedFileId) continue;

									final List<IFile> files = activeFileDownloadsAdapter.getFiles();
									for (IFile file : files) {
										if (file.getKey() != storedFile.getServiceId()) continue;

										activeFileDownloadsAdapter.remove(file);
										files.remove(file);
										break;
									}

									break;
								}
							}
						};

						localBroadcastManager.registerReceiver(onFileDownloadedReceiver, new IntentFilter(SyncService.onFileDownloadedEvent));

						if (onFileQueuedReceiver != null)
							localBroadcastManager.unregisterReceiver(onFileQueuedReceiver);

						onFileQueuedReceiver = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								final int storedFileId = intent.getIntExtra(SyncService.storedFileEventKey, -1);
								if (storedFileId == -1) return;

								for (StoredFile storedFile : storedFiles) {
									if (storedFile.getId() == storedFileId) return;
								}

								storedFileAccess.getStoredFile(storedFileId, new TwoParameterRunnable<FluentTask<Void,Void,StoredFile>, StoredFile>() {
									@Override
									public void run(FluentTask<Void, Void, StoredFile> owner, StoredFile storedFile) {
										if (storedFile == null) return;

										storedFiles.add(storedFile);
										activeFileDownloadsAdapter.add(new File(SessionConnection.getSessionConnectionProvider(), storedFile.getServiceId()));
									}
								});
							}
						};

						localBroadcastManager.registerReceiver(onFileQueuedReceiver, new IntentFilter(SyncService.onFileQueuedEvent));

						listView.setAdapter(activeFileDownloadsAdapter);

						progressBar.setVisibility(View.INVISIBLE);
						listView.setVisibility(View.VISIBLE);
					}
				});
			}
		});

		final Button toggleSyncButton = (Button) viewFileslayout.findViewById(R.id.toggleSyncButton);
		final CharSequence startSyncLabel = getActivity().getText(R.string.start_sync_button);
		final CharSequence stopSyncLabel = getActivity().getText(R.string.stop_sync_button);

		toggleSyncButton.setText(!SyncService.isSyncRunning() ? startSyncLabel : stopSyncLabel);

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				toggleSyncButton.setText(stopSyncLabel);
			}
		}, new IntentFilter(SyncService.onSyncStartEvent));

		localBroadcastManager.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				toggleSyncButton.setText(startSyncLabel);
			}
		}, new IntentFilter(SyncService.onSyncStopEvent));

		toggleSyncButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (SyncService.isSyncRunning())
					SyncService.cancelSync(v.getContext());
				else
					SyncService.doSync(v.getContext());
			}
		});

		toggleSyncButton.setEnabled(true);

		return viewFileslayout;
	}

	@Override
    public void onDestroy() {
        super.onDestroy();

        if (localBroadcastManager == null) return;

        if (onFileDownloadedReceiver != null)
            localBroadcastManager.unregisterReceiver(onFileDownloadedReceiver);

        if (onFileQueuedReceiver != null)
            localBroadcastManager.unregisterReceiver(onFileQueuedReceiver);
    }
}
