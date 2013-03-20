package com.lasthopesoftware.jrmediastreamer;

import jrAccess.JrSession;
import jrFileSystem.JrFile;
import jrFileSystem.JrItem;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewFiles extends FragmentActivity {

	public static final String ARG_ALBUM_POSITION = "album_position";   
	public static final String ARG_CATEGORY_POSITION = "category_position";
	public static final String ARG_ARTIST_POSITION = "artist_position";
	
	private JrItem mAlbum;
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);
        
        mAlbum = (JrItem) JrSession.selectedItem;
        
        FileListAdapter fileListAdapter = new FileListAdapter(this, mAlbum);
    	
    	ListView fileListView = (ListView)findViewById(R.id.lvFilelist);
    	fileListView.setAdapter(fileListAdapter);
	}
	
	public static class FileListAdapter extends BaseAdapter {
		private JrItem mAlbum;
		private Context mContext;
		
		public FileListAdapter(Context context, JrItem album) {
			mAlbum = album;
			mContext = context;
		}
		
		@Override
		public int getCount() {
			return mAlbum.getFiles().size();
		}
	
		@Override
		public Object getItem(int position) {
			return mAlbum.getFiles().get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return mAlbum.getFiles().get(position).getKey();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final JrFile file = mAlbum.getFiles().get(position);
			TextView tv = getGenericView(mContext);
			tv.setText(file.getValue());
			
			tv.setOnClickListener(new ListView.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					JrSession.playlist = mAlbum.getFiles();
					Intent svcIntent = new Intent(StreamingMusicService.ACTION_PLAY, Uri.parse(file.getUrl()), mContext, StreamingMusicService.class);
					mContext.startService(svcIntent);
				}
			});
			
			return tv;
		}
		
		public TextView getGenericView(Context context) {
	        // Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
		            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

	        TextView textView = new TextView(context);
	        textView.setTextAppearance(context, android.R.style.TextAppearance_Large);
	        textView.setLayoutParams(lp);
	        // Center the text vertically
	        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
//		        textView.setTextColor(getResources().getColor(marcyred));
	        // Set the text starting position        
	        textView.setPadding(20, 20, 20, 20);
	        return textView;
	    }

//		private ServiceConnection mConnection = new ServiceConnection() {
//
//			@Override
//			public void onServiceConnected(ComponentName name, IBinder service) {
//				// TODO Auto-generated method stub
//				ms = ((StreamingMusicService.StreamingMusicServiceBinder)service).getService();
//			}
//
//			@Override
//			public void onServiceDisconnected(ComponentName name) {
//				// TODO Auto-generated method stub
//				ms = null;
//			}
//		};
//		
//		private void doBindService() {
//			bindService()
//		}
	}
	
//	public class FileItem extends TextView {
//
//		public FileItem(Context context) {
//			super(context);
//			// TODO Auto-generated constructor stub
//		}
//		
//		@Override
//		public void onClick() {
//			
//		}
//	}

}
