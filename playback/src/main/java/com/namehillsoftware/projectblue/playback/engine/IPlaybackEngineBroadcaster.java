package com.namehillsoftware.projectblue.playback.engine;

import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackCompleted;
import com.lasthopesoftware.bluewater.client.playback.engine.events.OnPlaybackStarted;
import com.namehillsoftware.projectblue.playback.engine.events.OnPlayingFileChanged;
import com.namehillsoftware.projectblue.playback.engine.events.OnPlaylistReset;
import com.vedsoft.futures.runnables.OneParameterAction;

public interface IPlaybackEngineBroadcaster {
	IPlaybackEngineBroadcaster setOnPlayingFileChanged(OnPlayingFileChanged onPlayingFileChanged);

	IPlaybackEngineBroadcaster setOnPlaylistError(OneParameterAction<Throwable> onPlaylistError);

	IPlaybackEngineBroadcaster setOnPlaybackStarted(OnPlaybackStarted onPlaybackStarted);

	IPlaybackEngineBroadcaster setOnPlaybackCompleted(OnPlaybackCompleted onPlaybackCompleted);

	IPlaybackEngineBroadcaster setOnPlaylistReset(OnPlaylistReset onPlaylistReset);
}
