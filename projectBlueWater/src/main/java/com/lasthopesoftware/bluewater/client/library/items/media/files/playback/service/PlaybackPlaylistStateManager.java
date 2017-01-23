package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.MediaPlayerPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.PassThroughPromise;
import com.vedsoft.futures.callables.VoidFunc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

/**
 * Created by david on 1/22/17.
 */

class PlaybackPlaylistStateManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final Context context;
	private final int libraryId;
	private final IPositionedFileQueueProvider positionedFileQueueProvider;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<IFile> playlist;
	private PreparedPlaybackQueue preparedPlaybackQueue;
	private ConnectableObservable<PositionedPlaybackFile> observableProxy;
	private Disposable disposableFileChangedSubscription;

	PlaybackPlaylistStateManager(Context context, int libraryId, IPositionedFileQueueProvider positionedFileQueueProvider) {
		this.context = context;
		this.libraryId = libraryId;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
	}

	IPromise<Boolean> restorePlaylistFromStorage() {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.then((library, resolve, reject) -> {
					if (library == null) {
						resolve.withResult(false);
						return;
					}

					if (library.getSavedTracksString() == null || library.getSavedTracksString().isEmpty()) {
						resolve.withResult(false);
						return;
					}

					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.then(playlist -> {
							this.playlist = playlist;
							return library;
						})
						.then(VoidFunc.running(playlistPlayer -> resolve.withResult(true)))
						.error(VoidFunc.running(reject::withError));
				});
	}

	IPromise<Observable<PositionedPlaybackFile>> startPlaylist(final List<IFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		if (playlistPlayer != null) {
			try {
				playlistPlayer.close();
			} catch (IOException e) {
				logger.error("There was an error closing the playlist player", e);
			}
		}

		this.playlist = playlist;

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			updateLibraryPlaylist(playlistPosition, filePosition)
				.then(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(VoidFunc.running(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	IPromise<Observable<PositionedPlaybackFile>> changePosition(final int playlistPosition, final int filePosition) {
		final boolean wasPlaying = positionedPlaybackFile != null && positionedPlaybackFile.getPlaybackHandler().isPlaying();

		IPromise<Library> libraryPromise = updateLibraryPlaylist(playlistPosition, filePosition);

		if (!wasPlaying)
			return new ExpectedPromise<>(Observable::empty);

		logger.info("Position changed");
		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			libraryPromise
				.then(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(VoidFunc.running(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	void playRepeatedly() {
		persistLibraryRepeating(true)
			.then(VoidFunc.running(library -> {
				final IPositionedFileQueue cyclicalQueue = positionedFileQueueProvider.getCyclicalQueue(playlist, library.getNowPlayingId());

				preparedPlaybackQueue.updateQueue(cyclicalQueue);
			}));
	}

	void playToCompletion() {
		persistLibraryRepeating(false)
			.then(VoidFunc.running(library -> {
				if (playlistPlayer == null) return;

				final IPositionedFileQueue cyclicalQueue = positionedFileQueueProvider.getCompletableQueue(playlist, library.getNowPlayingId());

				preparedPlaybackQueue.updateQueue(cyclicalQueue);
			}));
	}

	IPromise<Observable<PositionedPlaybackFile>> resume() {
		if (playlistPlayer == null) return new ExpectedPromise<>(Observable::empty);

		playlistPlayer.resume();

		saveStateToLibrary();

		return new PassThroughPromise<>(observableProxy);
	}

	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();

		saveStateToLibrary();
	}


	public boolean isPlaying() {
		return playlistPlayer != null && playlistPlayer.isPlaying();
	}

	private Observable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) {
		if (disposableFileChangedSubscription != null && !disposableFileChangedSubscription.isDisposed())
			disposableFileChangedSubscription.dispose();

		observableProxy = Observable.create((playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition))).publish();
		disposableFileChangedSubscription =
			observableProxy.subscribe(p -> {
				positionedPlaybackFile = p;
				saveStateToLibrary();
			}, this::uncaughtExceptionHandler);

		return observableProxy;
	}

	public IPromise<Library> addFile(IFile file) {
		if (playlist != null)
			playlist.add(file);

		LibrarySession
			.getLibrary(context, libraryId)
			.thenPromise((result) -> {
				String newFileString = result.getSavedTracksString();
				if (!newFileString.endsWith(";")) newFileString += ";";
				newFileString += file.getKey() + ";";
				result.setSavedTracksString(newFileString);

				return LibrarySession.saveLibrary(context, result);
			});
	}

	private IPromise<Library> updateLibraryPlaylist(final int playlistPosition, final int filePosition) {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(result ->
					FileStringListUtilities
						.promiseSerializedFileStringList(playlist)
						.thenPromise(playlistString -> {
							result.setSavedTracksString(playlistString);
							result.setNowPlayingId(playlistPosition);
							result.setNowPlayingProgress(filePosition);

							return LibrarySession.saveLibrary(context, result);
						}));
	}

	private PreparedPlaybackQueue initializePreparedPlaybackQueue(Library library) {
		if (preparedPlaybackQueue != null) {
			try {
				preparedPlaybackQueue.close();
			} catch (IOException e) {
				logger.warn("There was an error closing the prepared playback queue", e);
			}
		}

		final IFileUriProvider uriProvider = new BestMatchUriProvider(context, SessionConnection.getSessionConnectionProvider(), library);

		final int startPosition = Math.max(library.getNowPlayingId(), 0);

		final IPositionedFileQueue positionedFileQueue =
			library.isRepeating()
				? positionedFileQueueProvider.getCyclicalQueue(playlist, startPosition)
				: positionedFileQueueProvider.getCompletableQueue(playlist, startPosition);

		preparedPlaybackQueue =
			new PreparedPlaybackQueue(
				new MediaPlayerPlaybackPreparerTaskFactory(
					uriProvider,
					new MediaPlayerInitializer(context, library)),
				positionedFileQueue);

		return preparedPlaybackQueue;
	}

	private IPromise<Library> persistLibraryRepeating(boolean isRepeating) {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.then(result -> {
					result.setRepeating(isRepeating);

					LibrarySession.saveLibrary(context, result);

					return result;
				});
	}

	private void saveStateToLibrary() {
		if (playlist == null) return;

		LibrarySession
			.getLibrary(context, libraryId)
			.then(VoidFunc.running(library -> {
				FileStringListUtilities
					.promiseSerializedFileStringList(playlist)
					.then(VoidFunc.running(savedTracksString -> {
						library.setSavedTracksString(savedTracksString);

						if (positionedPlaybackFile != null) {
							library.setNowPlayingId(positionedPlaybackFile.getPosition());
							library.setNowPlayingProgress(positionedPlaybackFile.getPlaybackHandler().getCurrentPosition());
						}

						LibrarySession.saveLibrary(context, library);
					}));
			}));
	}

	private void uncaughtExceptionHandler(Throwable exception) {
		if (exception instanceof MediaPlayerException) {
			saveStateToLibrary();
			return;
		}

		logger.error("An uncaught error has occurred!", exception);
	}

	@Override
	public void close() throws IOException {
		if (playlistPlayer != null)
			playlistPlayer.close();

		if (disposableFileChangedSubscription != null && !disposableFileChangedSubscription.isDisposed())
			disposableFileChangedSubscription.dispose();
	}
}