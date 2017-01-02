package com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.StatefulPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;

/**
 * Created by david on 12/7/16.
 */

public class ResolveablePlaybackHandler extends StatefulPlaybackHandler {

	private IResolvedPromise<IPlaybackHandler> resolve;

	public void resolve() {
		if (this.resolve != null)
			this.resolve.withResult(this);

		this.resolve = null;
	}

	@Override
	public IPromise<IPlaybackHandler> promisePlayback() {
		super.promisePlayback();
		return new Promise<>((resolve, reject) -> this.resolve = resolve);
	}
}
