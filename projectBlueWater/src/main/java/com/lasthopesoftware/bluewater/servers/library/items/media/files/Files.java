package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.os.AsyncTask;

import com.lasthopesoftware.threading.DataTask;
import com.lasthopesoftware.threading.IDataTask;
import com.lasthopesoftware.threading.IDataTask.OnCompleteListener;
import com.lasthopesoftware.threading.IDataTask.OnConnectListener;
import com.lasthopesoftware.threading.IDataTask.OnErrorListener;
import com.lasthopesoftware.threading.IDataTask.OnStartListener;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class Files implements IItemFiles {
	private final static Logger mLogger = LoggerFactory.getLogger(Files.class);
	
	private final String[] mParams;
	private OnStartListener<List<IFile>> mFileStartListener;
	private OnErrorListener<List<IFile>> mFileErrorListener;
	private IDataTask.OnCompleteListener<List<IFile>> mFileCompleteListener;
	public static final int GET_SHUFFLED = 1;

	private final OnConnectListener<List<IFile>> mFileConnectListener = new OnConnectListener<List<IFile>>() {
		
		@Override
		public List<IFile> onConnect(InputStream is) {
			ArrayList<IFile> files = new ArrayList<>();
			try {
				files = parseFileStringList(IOUtils.toString(is));				
			} catch (IOException e) {
				mLogger.error(e.toString(), e);
			}
			return files;
		}
	};
	
	public Files(String... fileParams) {
		mParams = new String[fileParams.length + 1];
		System.arraycopy(fileParams, 0, mParams, 0, fileParams.length);
		mParams[fileParams.length] = "Action=Serialize";
	}
	
	/* Required Methods for File Async retrieval */
	protected String[] getFileParams() {
		return mParams;
	}
	
	protected String[] getFileParams(final int option) {
		switch (option) {
			case GET_SHUFFLED:
				final String[] fileParams = new String[mParams.length + 1];
				System.arraycopy(mParams, 0, fileParams, 0, mParams.length);
				fileParams[mParams.length] = "Shuffle=1";
				return fileParams;
			default:
				return mParams;
		}
	}

	public void setOnFilesCompleteListener(OnCompleteListener<List<IFile>> listener) {
		mFileCompleteListener = listener;
	}

	public void setOnFilesStartListener(OnStartListener<List<IFile>> listener) {
		mFileStartListener = listener;
	}

	public void setOnFilesErrorListener(OnErrorListener<List<IFile>> listener) {
		mFileErrorListener = listener;
	}

	protected OnConnectListener<List<IFile>> getOnFileConnectListener() {
		return mFileConnectListener;
	}
	
	@Override
	public ArrayList<IFile> getFiles() {
		return getFiles(-1);
	}
	
	public void getFilesAsync() {
		getNewFilesTask().execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams());
	}
	
	@Override
	public ArrayList<IFile> getFiles(int option) {
		try {
			return (ArrayList<IFile>) getNewFilesTask().execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option)).get();
		} catch (Exception e) {
			mLogger.error(e.toString(), e);
			return getFiles();
		}
	}
	
	public final void getFileStringList(OnCompleteListener< String> onGetStringListComplete) {
		getFileStringList(onGetStringListComplete, null);
	}
	

	@Override
	public void getFileStringList(OnCompleteListener<String> onGetStringListComplete, OnErrorListener<String> onGetStringListError) {
		getFileStringList(-1, onGetStringListComplete, onGetStringListError);
	}
	
	public final void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete) {
		getFileStringList(option, onGetStringListComplete, null);
	}
	
	public void getFileStringList(final int option, final OnCompleteListener<String> onGetStringListComplete, final IDataTask.OnErrorListener<String> onGetStringListError) {
		final DataTask<String> getStringListTask = new DataTask<>(new OnConnectListener<String>() {
			
			@Override
			public String onConnect(InputStream is) {
				try {
					return IOUtils.toString(is);
				} catch (IOException e) {
					LoggerFactory.getLogger(Files.class).error(e.toString(), e);
					return null;
				}
			}
		});
		
		if (onGetStringListError != null)
			getStringListTask.addOnErrorListener(onGetStringListError);
		
		getStringListTask.addOnCompleteListener(onGetStringListComplete);
		getStringListTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, getFileParams(option));
	}

	protected DataTask<List<IFile>> getNewFilesTask() {
		final DataTask<List<IFile>> fileTask = new DataTask<>(getOnFileConnectListener());
		
		if (mFileCompleteListener != null)
			fileTask.addOnCompleteListener(mFileCompleteListener);
			
		if (mFileStartListener != null)
			fileTask.addOnStartListener(mFileStartListener);
		
		if (mFileErrorListener != null)
			fileTask.addOnErrorListener(mFileErrorListener);
		
		return fileTask;
	}

	public static final ArrayList<IFile> parseFileStringList(String fileList) {
		final String[] keys = fileList.split(";");
		
		final int offset = Integer.parseInt(keys[0]) + 1;
		final ArrayList<IFile> files = new ArrayList<>(Integer.parseInt(keys[1]));
		
		for (int i = offset; i < keys.length; i++) {
			if (keys[i].equals("-1")) continue;
			
			files.add(new File(Integer.parseInt(keys[i])));
		}
		
		return files;
	}
	
	public static final String serializeFileStringList(List<IFile> files) {
		final int fileSize = files.size();
		// Take a guess that most keys will not be greater than 8 characters and add some more
		// for the first characters
		final StringBuilder sb = new StringBuilder(fileSize * 9 + 8);
		sb.append("2;").append(fileSize).append(";-1;");
		
		for (IFile file : files)
			sb.append(file.getKey()).append(";");
		
		return sb.toString();
	}
}
