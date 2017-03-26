package com.lasthopesoftware.bluewater.client.playback.service.broadcasters;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackServiceFile;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by david on 2/11/17.
 */

public class PlaybackStartedBroadcaster implements CarelessOneParameterFunction<Observable<PositionedPlaybackServiceFile>, Observable<PositionedPlaybackServiceFile>> {

	private final PositionedPlaybackFileConsumer positionedPlaybackFileConsumer;
	private Disposable subscription;

	public PlaybackStartedBroadcaster(ISelectedLibraryIdentifierProvider libraryIdentifierProvider, IPlaybackBroadcaster playbackBroadcaster) {
		positionedPlaybackFileConsumer = new PositionedPlaybackFileConsumer(libraryIdentifierProvider, playbackBroadcaster);
	}

	@Override
	public Observable<PositionedPlaybackServiceFile> resultFrom(Observable<PositionedPlaybackServiceFile> observable) {
		if (subscription != null)
			subscription.dispose();

		subscription = observable.firstElement().subscribe(positionedPlaybackFileConsumer);

		return observable;
	}

	private static class PositionedPlaybackFileConsumer implements Consumer<PositionedPlaybackServiceFile> {
		private final ISelectedLibraryIdentifierProvider libraryIdentifierProvider;
		private final IPlaybackBroadcaster playbackBroadcaster;

		private PositionedPlaybackFileConsumer(ISelectedLibraryIdentifierProvider libraryIdentifierProvider, IPlaybackBroadcaster playbackBroadcaster) {
			this.libraryIdentifierProvider = libraryIdentifierProvider;
			this.playbackBroadcaster = playbackBroadcaster;
		}

		@Override
		public void accept(PositionedPlaybackServiceFile p) throws Exception {
			playbackBroadcaster.sendPlaybackBroadcast(PlaylistEvents.onPlaylistStart, libraryIdentifierProvider.getSelectedLibraryId(), p);
		}
	}
}
