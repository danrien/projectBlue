package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaybackFileProvider implements IPlaybackFileProvider {

	private final ArrayList<IFile> files;
	private final Context context;
	private final ConnectionProvider connectionProvider;
	
	private String mPlaylistString = null; 
	
	public PlaybackFileProvider(Context context, ConnectionProvider connectionProvider, List<IFile> files) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.files = files instanceof ArrayList<?> ? (ArrayList<IFile>)files : new ArrayList<>(files);
	}
	
	@Override
	public IPlaybackFile getNewPlaybackFile(int filePos) {
		return new PlaybackFile(context, connectionProvider, get(filePos));
	}

	@Override
	public int indexOf(IFile file) {
		return indexOf(0, file);
	}
	
	@Override
	public int indexOf(int startingIndex, IFile file) {
		for (int i = startingIndex; i < files.size(); i++) {
			if (files.get(i).equals(file)) return i;
		}
		
		return -1;
	}

	@Override
	public List<IFile> getFiles() {
		return Collections.unmodifiableList(files);
	}

	@Override
	public int size() {
		return files.size();
	}

	@Override
	public IFile get(int filePos) {
		return files.get(filePos);
	}

	@Override
	public boolean add(IFile file) {
		final boolean isAdded = files.add(file);
		mPlaylistString = null;
		return isAdded;
	}

	@Override
	public IFile remove(int filePos) {
		final IFile removedFile = files.remove(filePos);
		mPlaylistString = null;
		
		return removedFile;
	}
	
	@Override
	public String toPlaylistString() {
		if (mPlaylistString == null)
			mPlaylistString = Files.serializeFileStringList(files);
		
		return mPlaylistString;
	}
}
