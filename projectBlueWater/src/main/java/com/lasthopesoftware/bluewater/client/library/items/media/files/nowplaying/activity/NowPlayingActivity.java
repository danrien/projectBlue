package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.client.connection.helpers.PollConnection;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list.NowPlayingFilesListActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesStorage;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.library.items.media.image.ImageProvider;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents;
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.TrackPositionBroadcaster;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.bluewater.shared.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.view.ViewUtils;
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.lazyj.AbstractThreadLocalLazy;
import com.vedsoft.lazyj.ILazy;
import com.vedsoft.lazyj.Lazy;

import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TimerTask;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class NowPlayingActivity extends AppCompatActivity {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NowPlayingActivity.class);

	private static IPromise<Bitmap> getFileImageTask;

	public static void startNowPlayingActivity(final Context context) {
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
	}

	private final ILazy<Handler> messageHandler = new Lazy<>(() -> new Handler(getMainLooper()));

	private final LazyViewFinder<ImageButton> playButton = new LazyViewFinder<>(this, R.id.btnPlay);
	private final LazyViewFinder<ImageButton> pauseButton = new LazyViewFinder<>(this, R.id.btnPause);
	private final LazyViewFinder<RatingBar> songRating = new LazyViewFinder<>(this, R.id.rbSongRating);
	private final LazyViewFinder<RelativeLayout> contentView = new LazyViewFinder<>(this, R.id.viewNowPlayingRelativeLayout);
	private final LazyViewFinder<ProgressBar> songProgressBar = new LazyViewFinder<>(this, R.id.pbNowPlaying);
	private final LazyViewFinder<ProgressBar> loadingImg = new LazyViewFinder<>(this, R.id.pbLoadingImg);
	private final LazyViewFinder<ImageView> nowPlayingImageViewFinder = new LazyViewFinder<>(this, R.id.imgNowPlaying);
	private final LazyViewFinder<TextView> nowPlayingArtist = new LazyViewFinder<>(this, R.id.tvSongArtist);
	private final LazyViewFinder<ImageButton> isScreenKeptOnButton = new LazyViewFinder<>(this, R.id.isScreenKeptOnButton);
	private final LazyViewFinder<TextView> nowPlayingTitle = new LazyViewFinder<>(this, R.id.tvSongTitle);
	private final ILazy<NowPlayingToggledVisibilityControls> nowPlayingToggledVisibilityControls = new AbstractThreadLocalLazy<NowPlayingToggledVisibilityControls>() {
		@Override
		protected NowPlayingToggledVisibilityControls initialize() throws Exception {
			return new NowPlayingToggledVisibilityControls(new LazyViewFinder<>(NowPlayingActivity.this, R.id.llNpButtons), new LazyViewFinder<>(NowPlayingActivity.this, R.id.menuControlsLinearLayout), songRating);
		}
	};
	private final ILazy<INowPlayingRepository> lazyNowPlayingRepository = new AbstractThreadLocalLazy<INowPlayingRepository>() {
		@Override
		protected INowPlayingRepository initialize() throws Exception {
			final LibraryRepository libraryRepository = new LibraryRepository(NowPlayingActivity.this);

			return
				new NowPlayingRepository(
					new SpecificLibraryProvider(
						new SelectedBrowserLibraryIdentifierProvider(NowPlayingActivity.this).getSelectedLibraryId(),
						libraryRepository),
					libraryRepository);
		}
	};

	private TimerTask timerTask;

	private LocalBroadcastManager localBroadcastManager;

	private static ViewStructure viewStructure;

	private static final String fileNotFoundError = "The file %1s was not found!";

	private static boolean isScreenKeptOn;

	private final Runnable onConnectionLostListener = () -> WaitForConnectionDialog.show(NowPlayingActivity.this);

	private final BroadcastReceiver onPlaybackChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int playlistPosition = intent.getIntExtra(PlaylistEvents.PlaylistParameters.playlistPosition, -1);
			if (playlistPosition < 0) return;

			showNowPlayingControls();
			updateKeepScreenOnStatus();

			setView(playlistPosition);
		}
	};

	private final BroadcastReceiver onPlaybackStartedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			togglePlayingButtons(true);

			updateKeepScreenOnStatus();
		}
	};

	private final BroadcastReceiver onPlaybackStoppedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			togglePlayingButtons(false);
			disableKeepScreenOn();
		}
	};

	private final BroadcastReceiver onTrackPositionChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final int fileDuration = intent.getIntExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration,-1);
			if (fileDuration > -1) setTrackDuration(fileDuration);

			final int filePosition = intent.getIntExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
			if (filePosition > -1) setTrackProgress(filePosition);
		}
	};

	private static class ViewStructure {
		final UrlKeyHolder<Integer> urlKeyHolder;
		final IFile file;
		Map<String, String> fileProperties;
		Bitmap nowPlayingImage;
		int filePosition;
		int fileDuration;
		
		ViewStructure(UrlKeyHolder<Integer> urlKeyHolder, IFile file) {
			this.urlKeyHolder = urlKeyHolder;
			this.file = file;
		}
		
		void release() {
			if (nowPlayingImage != null)
				nowPlayingImage.recycle();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_view_now_playing);

		contentView.findView().setOnClickListener(v -> showNowPlayingControls());

		nowPlayingToggledVisibilityControls.getObject().toggleVisibility(false);

		final IntentFilter playbackStoppedIntentFilter = new IntentFilter();
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistPause);
		playbackStoppedIntentFilter.addAction(PlaylistEvents.onPlaylistStop);

		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		localBroadcastManager.registerReceiver(onPlaybackStoppedReceiver, playbackStoppedIntentFilter);
		localBroadcastManager.registerReceiver(onPlaybackStartedReceiver, new IntentFilter(PlaylistEvents.onPlaylistStart));
		localBroadcastManager.registerReceiver(onPlaybackChangedReceiver, new IntentFilter(PlaylistEvents.onPlaylistChange));
		localBroadcastManager.registerReceiver(onTrackPositionChanged, new IntentFilter(TrackPositionBroadcaster.trackPositionUpdate));

		PollConnection.Instance.get(this).addOnConnectionLostListener(onConnectionLostListener);
		
		playButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.play(v.getContext());
			playButton.findView().setVisibility(View.INVISIBLE);
			pauseButton.findView().setVisibility(View.VISIBLE);
		});
		
		pauseButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.pause(v.getContext());
			playButton.findView().setVisibility(View.VISIBLE);
			pauseButton.findView().setVisibility(View.INVISIBLE);
		});

		final ImageButton next = (ImageButton) findViewById(R.id.btnNext);
		if (next != null) {
			next.setOnClickListener(v -> {
				if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
				PlaybackService.next(v.getContext());
			});
		}

		final ImageButton previous = (ImageButton) findViewById(R.id.btnPrevious);
		if (previous != null) {
			previous.setOnClickListener(v -> {
				if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
				PlaybackService.previous(v.getContext());
			});
		}

		final ImageButton shuffleButton = (ImageButton) findViewById(R.id.repeatButton);
		setRepeatingIcon(shuffleButton);

		if (shuffleButton != null) {
			shuffleButton.setOnClickListener(v ->
				lazyNowPlayingRepository.getObject()
					.getNowPlaying()
					.then(Dispatch.toHandler(result -> {
						final boolean isRepeating = !result.isRepeating;
						if (isRepeating)
							PlaybackService.setRepeating(v.getContext());
						else
							PlaybackService.setCompleting(v.getContext());

						setRepeatingIcon(shuffleButton, isRepeating);

						return result;
					}, messageHandler.getObject())));
		}

		final ImageButton viewNowPlayingListButton = (ImageButton) findViewById(R.id.viewNowPlayingListButton);
		if (viewNowPlayingListButton != null)
			viewNowPlayingListButton.setOnClickListener(v -> startActivity(new Intent(v.getContext(), NowPlayingFilesListActivity.class)));

		isScreenKeptOnButton.findView().setOnClickListener(v -> {
			isScreenKeptOn = !isScreenKeptOn;
			updateKeepScreenOnStatus();
		});

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			songProgressBar.findView().getProgressDrawable().setColorFilter(getResources().getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN);

		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.then(Dispatch.toHandler(runCarelessly(np -> {
				final IFile file = np.playlist.get(np.playlistPosition);

				if (viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(SessionConnection.getSessionConnectionProvider().getUrlProvider().getBaseUrl(), file.getKey()))) {
					setView(viewStructure.file, viewStructure.filePosition);
					return;
				}

				setView(np.playlist.get(np.playlistPosition), np.filePosition);
			}), messageHandler.getObject()));
	}
	
	@Override
	public void onStart() {
		super.onStart();

		updateKeepScreenOnStatus();

		if (!InstantiateSessionConnectionActivity.restoreSessionConnection(this)) initializeView();

		bindService(new Intent(this, PlaybackService.class), new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				togglePlayingButtons(((PlaybackService)(((GenericBinder<?>)service).getService())).isPlaying());
				unbindService(this);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
		}, BIND_AUTO_CREATE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) initializeView();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initializeView() {
		playButton.findView().setVisibility(View.VISIBLE);
		pauseButton.findView().setVisibility(View.INVISIBLE);

		if (viewStructure != null)
			setView(viewStructure.file, viewStructure.filePosition);
	}

	private void setRepeatingIcon(final ImageButton imageButton) {
		setRepeatingIcon(imageButton, false);
		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.then(Dispatch.toHandler(runCarelessly(result -> {
				if (result != null)
					setRepeatingIcon(imageButton, result.isRepeating);
			}), messageHandler.getObject()));
	}
	
	private static void setRepeatingIcon(final ImageButton imageButton, boolean isRepeating) {
		imageButton.setImageDrawable(ViewUtils.getDrawable(imageButton.getContext(), isRepeating ? R.drawable.av_repeat_dark : R.drawable.av_no_repeat_dark));
	}

	private void updateKeepScreenOnStatus() {
		isScreenKeptOnButton.findView().setImageDrawable(ViewUtils.getDrawable(this, isScreenKeptOn ? R.drawable.screen_on : R.drawable.screen_off));

		if (isScreenKeptOn)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			disableKeepScreenOn();
	}

	private void disableKeepScreenOn() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void togglePlayingButtons(boolean isPlaying) {
		playButton.findView().setVisibility(ViewUtils.getVisibility(!isPlaying));
		pauseButton.findView().setVisibility(ViewUtils.getVisibility(isPlaying));
	}

	private void setView(final int playlistPosition) {
		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.then(Dispatch.toHandler(runCarelessly(np -> {
				if (playlistPosition >= np.playlist.size()) return;

				final IFile file = np.playlist.get(playlistPosition);

				final int filePosition =
					viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(SessionConnection.getSessionConnectionProvider().getUrlProvider().getBaseUrl(), file.getKey()))
						? viewStructure.filePosition
						: 0;

				setView(file, filePosition);
			}), messageHandler.getObject()))
			.error(runCarelessly(e -> logger.error("An error occurred while getting the Now Playing data", e)));
	}
	
	private void setView(final IFile file, final int initialFilePosition) {
		final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(SessionConnection.getSessionConnectionProvider().getUrlProvider().getBaseUrl(), file.getKey());

		if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
			viewStructure.release();
			viewStructure = null;
		}
		
		if (viewStructure == null)
			viewStructure = new ViewStructure(urlKeyHolder, file);
		
		final ViewStructure viewStructure = NowPlayingActivity.viewStructure;

		final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();
		if (viewStructure.nowPlayingImage == null) {
			try {				
				// Cancel the getFileImageTask if it is already in progress
				if (getFileImageTask != null)
					getFileImageTask.cancel();
				
				nowPlayingImage.setVisibility(View.INVISIBLE);
				loadingImg.findView().setVisibility(View.VISIBLE);

				final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
				final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();

				getFileImageTask =
					ImageProvider
						.getImage(
							this,
							connectionProvider,
							new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache)),
							file.getKey());

				getFileImageTask
					.then(Dispatch.toContext(runCarelessly(bitmap -> {
						if (viewStructure.nowPlayingImage != null)
							viewStructure.nowPlayingImage.recycle();
						viewStructure.nowPlayingImage = bitmap;

						nowPlayingImage.setImageBitmap(bitmap);

						displayImageBitmap();
					}), this))
					.error(runCarelessly(e -> logger.error("There was an error retrieving file details", e)));
				
			} catch (Exception e) {
				logger.error(e.toString(), e);
			}
		} else {
			nowPlayingImage.setImageBitmap(viewStructure.nowPlayingImage);
			displayImageBitmap();
		}

		if (viewStructure.fileProperties != null) {
			setFileProperties(file, initialFilePosition, viewStructure.fileProperties);
			return;
		}

		disableViewWithMessage(R.string.lbl_loading);

		final FilePropertiesProvider filePropertiesProvider = new FilePropertiesProvider(SessionConnection.getSessionConnectionProvider(), FilePropertyCache.getInstance());
		filePropertiesProvider
			.promiseFileProperties(file.getKey())
			.then(Dispatch.toHandler(runCarelessly(fileProperties -> {
				viewStructure.fileProperties = fileProperties;
				setFileProperties(file, initialFilePosition, fileProperties);
			}), messageHandler.getObject()))
			.error(Dispatch.toHandler(exception -> handleIoException(file, initialFilePosition, exception), messageHandler.getObject()));
	}

	private void setFileProperties(final IFile file, final int initialFilePosition, Map<String, String> fileProperties) {
		final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
		nowPlayingArtist.findView().setText(artist);

		final String title = fileProperties.get(FilePropertiesProvider.NAME);
		nowPlayingTitle.findView().setText(title);
		nowPlayingTitle.findView().setSelected(true);

		Float fileRating = null;
		final String stringRating = fileProperties.get(FilePropertiesProvider.RATING);
		try {
			if (stringRating != null && !stringRating.isEmpty())
				fileRating = Float.valueOf(stringRating);
		} catch (NumberFormatException e) {
			logger.info("Failed to parse rating", e);
		}

		setFileRating(file, fileRating);

		final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

		setTrackDuration(duration > 0 ? duration : 100);
		setTrackProgress(initialFilePosition);
	}

	private void setFileRating(IFile file, Float rating) {
		final RatingBar songRatingBar = songRating.findView();
		songRatingBar.setRating(rating != null ? rating : 0f);

		songRatingBar.setOnRatingBarChangeListener((ratingBar, newRating, fromUser) -> {
			if (!fromUser || !nowPlayingToggledVisibilityControls.getObject().isVisible())
				return;

			final String stringRating = String.valueOf(Math.round(newRating));
			FilePropertiesStorage.storeFileProperty(SessionConnection.getSessionConnectionProvider(), file.getKey(), FilePropertiesProvider.RATING, stringRating);
			viewStructure.fileProperties.put(FilePropertiesProvider.RATING, stringRating);
		});

		songRatingBar.setEnabled(true);
	}

	private void setTrackDuration(int duration) {
		songProgressBar.findView().setMax(duration);

		if (viewStructure != null)
			viewStructure.fileDuration = duration;
	}

	private void setTrackProgress(int progress) {
		songProgressBar.findView().setProgress(progress);

		if (viewStructure != null)
			viewStructure.filePosition = progress;
	}

	private boolean handleFileNotFoundException(IFile file, FileNotFoundException fe) {
		logger.error(String.format(fileNotFoundError, file), fe);
		disableViewWithMessage(R.string.file_not_found);
		return true;
	}
	
	private boolean handleIoException(IFile file, int position, Throwable exception) {
		if (exception instanceof FileNotFoundException)
			return handleFileNotFoundException(file, (FileNotFoundException)exception);

		if (exception instanceof IOException) {
			resetViewOnReconnect(file, position);
			return true;
		}
		
		return false;
	}
	
	private void displayImageBitmap() {
		nowPlayingImageViewFinder.findView().setScaleType(ScaleType.CENTER_CROP);
		loadingImg.findView().setVisibility(View.INVISIBLE);
		nowPlayingImageViewFinder.findView().setVisibility(View.VISIBLE);
	}

	private void showNowPlayingControls() {
		nowPlayingToggledVisibilityControls.getObject().toggleVisibility(true);
		contentView.findView().invalidate();

		if (timerTask != null) timerTask.cancel();
		timerTask = new TimerTask() {
			boolean cancelled;

			@Override
			public void run() {
				if (!cancelled)
					nowPlayingToggledVisibilityControls.getObject().toggleVisibility(false);
			}

			@Override
			public boolean cancel() {
				cancelled = true;
				return super.cancel();
			}
		};

		messageHandler.getObject().postDelayed(timerTask, 5000);
	}
	
	private void resetViewOnReconnect(final IFile file, final int position) {
		PollConnection.Instance.get(this).addOnConnectionRegainedListener(() -> setView(file, position));
		WaitForConnectionDialog.show(this);
	}

	private void disableViewWithMessage(@StringRes int messageId) {
		nowPlayingTitle.findView().setText(messageId);
		nowPlayingArtist.findView().setText("");
		songRating.findView().setRating(0);
		songRating.findView().setEnabled(false);
	}

	@Override
	protected void onStop() {
		super.onStop();

		disableKeepScreenOn();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (timerTask != null) timerTask.cancel();

		localBroadcastManager.unregisterReceiver(onPlaybackStoppedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackStartedReceiver);
		localBroadcastManager.unregisterReceiver(onPlaybackChangedReceiver);
		localBroadcastManager.unregisterReceiver(onTrackPositionChanged);

		PollConnection.Instance.get(this).removeOnConnectionLostListener(onConnectionLostListener);
	}
}
