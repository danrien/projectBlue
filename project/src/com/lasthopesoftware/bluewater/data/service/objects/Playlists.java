package com.lasthopesoftware.bluewater.data.service.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.data.service.access.PlaylistRequest;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnConnectListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnErrorListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnStartListener;


public class Playlists extends ItemAsyncBase implements IItem {

	private SparseArray<Playlist> mMappedPlaylists;
	private final ArrayList<OnStartListener<List<Playlist>>> mItemStartListeners = new ArrayList<OnStartListener<List<Playlist>>>(1);
	private final ArrayList<OnErrorListener<List<Playlist>>> mItemErrorListeners = new ArrayList<OnErrorListener<List<Playlist>>>(1);
	private ArrayList<OnCompleteListener<List<Playlist>>> mOnCompleteListeners;
	
	private final OnConnectListener<List<Playlist>> mOnConnectListener = new OnConnectListener<List<Playlist>>() {
		
		@Override
		public List<Playlist> onConnect(InputStream is) {
			ArrayList<Playlist> streamResult = PlaylistRequest.GetItems(is);
			
			int i = 0;
			while (i < streamResult.size()) {
				if (streamResult.get(i).getParent() != null) streamResult.remove(i);
				else i++;
			}
			return streamResult;
		}
	};
	
	public Playlists(int key) {
		setKey(key);
		setValue("Playlist");
	}
	
	public SparseArray<Playlist> getMappedPlaylists() {
		if (mMappedPlaylists == null) denormalizeAndMap();
		return mMappedPlaylists;
	}
	
	private void denormalizeAndMap() {
		try {
			mMappedPlaylists = new SparseArray<Playlist>(getSubItems().size());
			denormalizeAndMap(getSubItems());
		} catch (IOException io) {
			LoggerFactory.getLogger(Playlists.class).error(io.getMessage(), io);
		}
	}
	
	private void denormalizeAndMap(ArrayList<Playlist> items) {
		for (Playlist playlist : items) {
			mMappedPlaylists.append(playlist.getKey(), playlist);
//			if (playlist.getSubItems().size() > 0) denormalizeAndMap(playlist.getSubItems());
		}
	}
	
	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Playlists/List" };
	}

	@Override
	public void addOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
		if (mOnCompleteListeners == null) mOnCompleteListeners = new ArrayList<OnCompleteListener<List<Playlist>>>();
		
		mOnCompleteListeners.add(listener);
	}

	@Override
	public void removeOnItemsCompleteListener(OnCompleteListener<List<Playlist>> listener) {
		if (mOnCompleteListeners != null)
			mOnCompleteListeners.remove(listener);
	}

	@Override
	protected OnConnectListener<List<Playlist>> getOnItemConnectListener() {
		return mOnConnectListener;
	}

	@Override
	protected List<OnCompleteListener<List<Playlist>>> getOnItemsCompleteListeners() {
		return mOnCompleteListeners;
	}

	@Override
	protected List<OnStartListener<List<Playlist>>> getOnItemsStartListeners() {
		return mItemStartListeners;
	}

	@Override
	protected List<OnErrorListener<List<Playlist>>> getOnItemsErrorListeners() {
		return mItemErrorListeners;
	}
}
