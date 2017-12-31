package com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.service.receivers.devices.remote.connected.IConnectedDeviceBroadcaster;
import com.vedsoft.futures.runnables.TwoParameterAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;

public class RemoteControlProxy extends BroadcastReceiver {

	private final Collection<IConnectedDeviceBroadcaster> connectedDeviceBroadcasters;
	private final Map<String, TwoParameterAction<Intent, IConnectedDeviceBroadcaster>> mappedEvents;

	public RemoteControlProxy(IConnectedDeviceBroadcaster... connectedDeviceBroadcasters) {
		this.connectedDeviceBroadcasters = Arrays.asList(connectedDeviceBroadcasters);

		mappedEvents = new HashMap<>(5);
		mappedEvents.put(PlaylistEvents.onPlaylistChange, this::onPlaylistChange);
		mappedEvents.put(PlaylistEvents.onPlaylistPause, (i, cd) -> cd.setPaused());
		mappedEvents.put(PlaylistEvents.onPlaylistStart, (i, cd) -> cd.setPlaying());
		mappedEvents.put(PlaylistEvents.onPlaylistStop, (i, cd) -> cd.setStopped());
		mappedEvents.put(TrackPositionBroadcaster.trackPositionUpdate, this::onTrackPositionUpdate);
	}

	public Set<String> registerForIntents() {
		return mappedEvents.keySet();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action == null) return;

		final TwoParameterAction<Intent, IConnectedDeviceBroadcaster> eventHandler = mappedEvents.get(action);
		if (eventHandler == null) return;

		for (final IConnectedDeviceBroadcaster connectedDeviceBroadcaster : connectedDeviceBroadcasters)
			eventHandler.runWith(intent, connectedDeviceBroadcaster);
	}

	private void onPlaylistChange(Intent intent, final IConnectedDeviceBroadcaster connectedDeviceBroadcaster) {
		final int fileKey = intent.getIntExtra(PlaylistEvents.PlaybackFileParameters.fileKey, -1);
		if (fileKey > -1)
			connectedDeviceBroadcaster.updateNowPlaying(new ServiceFile(fileKey));
	}

	private void onTrackPositionUpdate(Intent intent, final IConnectedDeviceBroadcaster connectedDeviceBroadcaster) {
		final long trackPosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
		if (trackPosition > -1)
			connectedDeviceBroadcaster.updateTrackPosition(trackPosition);
	}
}
