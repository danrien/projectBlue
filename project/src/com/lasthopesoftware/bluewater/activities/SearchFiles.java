package com.lasthopesoftware.bluewater.activities;

import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.filelist.FileListAdapter;
import com.lasthopesoftware.bluewater.activities.common.HandleViewIoException;
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.common.ViewUtils;
import com.lasthopesoftware.bluewater.activities.listeners.ClickFileListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.threading.ISimpleTask;

public class SearchFiles extends FragmentActivity {

	private ProgressBar pbLoading;
	private ListView fileListView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_files);
        fileListView = (ListView)findViewById(R.id.lvFilelist);
        pbLoading = (ProgressBar)findViewById(R.id.pbLoadingFileList);
        
        fileListView.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        
        handleIntent(getIntent());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return ViewUtils.buildStandardMenu(this, menu);
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
	
	@SuppressWarnings("unchecked")
	private void handleIntent(Intent intent) {
		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) return;
        
		final String query = intent.getStringExtra(SearchManager.QUERY);
        if (query == null || query.isEmpty()) return;

        setTitle(String.format(getString(R.string.title_activity_search_results), query));
        
		final Files filesContainer = new Files("Files/Search", "Query=" + query);
        final SearchFiles _this = this;
        filesContainer.setOnFilesCompleteListener(new IDataTask.OnCompleteListener<List<File>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, List<File>> owner, List<File> result) {
				if (result == null) return;
				
				final FileListAdapter fileListAdapter = new FileListAdapter(_this, R.id.tvStandard, result);
		    	fileListView.setOnItemClickListener(new ClickFileListener(filesContainer));
		    	fileListView.setOnItemLongClickListener(new LongClickFlipListener());
		    	fileListView.setAdapter(fileListAdapter);
		    	
		    	
			}
		});
        
        filesContainer.setOnFilesErrorListener(new HandleViewIoException(_this, new OnConnectionRegainedListener() {
			
				@Override
				public void onConnectionRegained() {
					filesContainer.getFilesAsync();
				}
			})
        );
                
        fileListView.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.INVISIBLE);
        
        filesContainer.getFilesAsync();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnection.restoreSessionConnection(this);
	}

}
