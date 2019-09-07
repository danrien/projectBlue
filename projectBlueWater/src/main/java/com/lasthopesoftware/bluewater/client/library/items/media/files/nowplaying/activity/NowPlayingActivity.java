package com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter;
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService;
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog;
import com.lasthopesoftware.bluewater.client.connection.session.InstantiateSessionConnectionActivity;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.access.SpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.disk.AndroidDiskCacheDirectoryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.list.NowPlayingFilesListActivity;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
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
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePalette;
import com.lasthopesoftware.bluewater.shared.android.colors.MediaStylePaletteProvider;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.android.view.ViewUtils;
import com.lasthopesoftware.bluewater.shared.exceptions.UnexpectedExceptionToaster;
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;

public class NowPlayingActivity extends AppCompatActivity {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(NowPlayingActivity.class);

	public static void startNowPlayingActivity(final Context context) {
		final Intent viewIntent = new Intent(context, NowPlayingActivity.class);
		viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(viewIntent);
	}


	private static boolean isScreenKeptOn;

	private static ViewStructure viewStructure;

	private static Bitmap nowPlayingBackgroundBitmap;

	private final CreateAndHold<Handler> messageHandler = new Lazy<>(() -> new Handler(getMainLooper()));

	private final LazyViewFinder<ImageButton> playButton = new LazyViewFinder<>(this, R.id.btnPlay);
	private final LazyViewFinder<ImageButton> pauseButton = new LazyViewFinder<>(this, R.id.btnPause);
	private final LazyViewFinder<ImageButton> nextButton = new LazyViewFinder<>(this, R.id.btnNext);
	private final LazyViewFinder<ImageButton> previousButton = new LazyViewFinder<>(this, R.id.btnPrevious);
	private final LazyViewFinder<RatingBar> songRating = new LazyViewFinder<>(this, R.id.rbSongRating);
	private final LazyViewFinder<RelativeLayout> contentView = new LazyViewFinder<>(this, R.id.viewNowPlayingRelativeLayout);
	private final LazyViewFinder<ProgressBar> songProgressBar = new LazyViewFinder<>(this, R.id.pbNowPlaying);
	private final LazyViewFinder<ImageView> nowPlayingImageViewFinder = new LazyViewFinder<>(this, R.id.imgNowPlaying);
	private final LazyViewFinder<TextView> nowPlayingArtist = new LazyViewFinder<>(this, R.id.tvSongArtist);
	private final LazyViewFinder<ImageButton> isScreenKeptOnButton = new LazyViewFinder<>(this, R.id.isScreenKeptOnButton);
	private final LazyViewFinder<TextView> nowPlayingTitle = new LazyViewFinder<>(this, R.id.tvSongTitle);
	private final LazyViewFinder<ImageView> nowPlayingImageLoading = new LazyViewFinder<>(this, R.id.imgNowPlayingLoading);
	private final LazyViewFinder<ProgressBar> loadingProgressBar = new LazyViewFinder<>(this, R.id.pbLoadingImg);
	private final LazyViewFinder<RelativeLayout> nowPlayingLayout = new LazyViewFinder<>(this, R.id.rlCtlNowPlaying);
	private final LazyViewFinder<ImageButton> viewNowPlayingListButton = new LazyViewFinder<>(this, R.id.viewNowPlayingListButton);
	private final LazyViewFinder<ImageButton> shuffleButton = new LazyViewFinder<>(this, R.id.repeatButton);
	private final Lazy<Drawable> playButtonDrawable = new Lazy<>(() -> {
		final Drawable copy = playButton.findView().getDrawable().getConstantState().newDrawable().mutate();
		playButton.findView().setImageDrawable(copy);
		return copy;
	});
	private final Lazy<Drawable> repeatDrawable = new Lazy<>(() -> ContextCompat.getDrawable(this, R.drawable.av_repeat_dark));
	private final Lazy<Drawable> shuffleDrawable = new Lazy<>(() -> ContextCompat.getDrawable(this, R.drawable.av_no_repeat_dark));
	private final Lazy<Drawable> screenOnDrawable = new Lazy<>(() -> ContextCompat.getDrawable(this, R.drawable.screen_on));
	private final Lazy<Drawable> screenOffDrawable = new Lazy<>(() -> ContextCompat.getDrawable(this, R.drawable.screen_off));

	private final Lazy<Drawable[]> tintableDrawables = new Lazy<>(() -> new Drawable[] {
		playButtonDrawable.getObject(),
		pauseButton.findView().getDrawable(),
		nextButton.findView().getDrawable(),
		previousButton.findView().getDrawable(),
		songRating.findView().getProgressDrawable(),
		songProgressBar.findView().getProgressDrawable(),
		viewNowPlayingListButton.findView().getDrawable(),
		repeatDrawable.getObject(),
		shuffleDrawable.getObject(),
		screenOnDrawable.getObject(),
		screenOffDrawable.getObject()
	});

	private final CreateAndHold<NowPlayingToggledVisibilityControls> nowPlayingToggledVisibilityControls = new AbstractSynchronousLazy<NowPlayingToggledVisibilityControls>() {
		@Override
		protected NowPlayingToggledVisibilityControls create() {
			return new NowPlayingToggledVisibilityControls(new LazyViewFinder<>(NowPlayingActivity.this, R.id.llNpButtons), new LazyViewFinder<>(NowPlayingActivity.this, R.id.menuControlsLinearLayout), songRating);
		}
	};
	private final CreateAndHold<INowPlayingRepository> lazyNowPlayingRepository = new AbstractSynchronousLazy<INowPlayingRepository>() {
		@Override
		protected INowPlayingRepository create() {
			final LibraryRepository libraryRepository = new LibraryRepository(NowPlayingActivity.this);

			return
				new NowPlayingRepository(
					new SpecificLibraryProvider(
						new SelectedBrowserLibraryIdentifierProvider(NowPlayingActivity.this).getSelectedLibraryId(),
						libraryRepository),
					libraryRepository);
		}
	};
	private final Runnable onConnectionLostListener = () -> WaitForConnectionDialog.show(this);

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
			final long fileDuration = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.fileDuration,-1);
			if (fileDuration > -1) setTrackDuration(fileDuration);

			final long filePosition = intent.getLongExtra(TrackPositionBroadcaster.TrackPositionChangedParameters.filePosition, -1);
			if (filePosition > -1) setTrackProgress(filePosition);
		}
	};

	private final CreateAndHold<Promise<ImageProvider>> lazyImageProvider = new AbstractSynchronousLazy<Promise<ImageProvider>>() {
		@Override
		protected Promise<ImageProvider> create() {
			return SessionConnection.getInstance(NowPlayingActivity.this).promiseSessionConnection()
				.then(connectionProvider -> {
					final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();

					return new ImageProvider(
						NowPlayingActivity.this,
						connectionProvider,
						new AndroidDiskCacheDirectoryProvider(NowPlayingActivity.this),
						new CachedFilePropertiesProvider(connectionProvider, filePropertyCache,
							new FilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance())));
				});
		}
	};

	private final Lazy<MediaStylePaletteProvider> lazyMediaStyleProvider = new Lazy<>(() -> new MediaStylePaletteProvider(this));

	private final CreateAndHold<Promise<Bitmap>> lazyDefaultBitmap = new AbstractSynchronousLazy<Promise<Bitmap>>() {
		@Override
		protected Promise<Bitmap> create() {
			final DefaultImageProvider defaultImageProvider = new DefaultImageProvider(NowPlayingActivity.this);
			return defaultImageProvider.promiseFileBitmap();
		}
	};

	private final CreateAndHold<Promise<MediaStylePalette>> lazyDefaultPalette = new Lazy<>(() ->
		lazyDefaultBitmap.getObject().eventually(b -> lazyMediaStyleProvider.getObject().promisePalette(b)));

	private TimerTask timerTask;

	private LocalBroadcastManager localBroadcastManager;

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

		PollConnectionService.addOnConnectionLostListener(onConnectionLostListener);

		setNowPlayingBackgroundBitmap();

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

		nextButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.next(v.getContext());
		});

		previousButton.findView().setOnClickListener(v -> {
			if (!nowPlayingToggledVisibilityControls.getObject().isVisible()) return;
			PlaybackService.previous(v.getContext());
		});

		setRepeatingIcon(shuffleButton.findView());

		shuffleButton.findView().setOnClickListener(v ->
			lazyNowPlayingRepository.getObject()
				.getNowPlaying()
				.eventually(LoopedInPromise.<NowPlaying, NowPlaying>response(result -> {
					final boolean isRepeating = !result.isRepeating;
					if (isRepeating)
						PlaybackService.setRepeating(v.getContext());
					else
						PlaybackService.setCompleting(v.getContext());

					setRepeatingIcon(shuffleButton.findView(), isRepeating);

					return result;
				}, messageHandler.getObject())));

		viewNowPlayingListButton.findView().setOnClickListener(v -> startActivity(new Intent(v.getContext(), NowPlayingFilesListActivity.class)));

		isScreenKeptOnButton.findView().setOnClickListener(v -> {
			isScreenKeptOn = !isScreenKeptOn;
			updateKeepScreenOnStatus();
		});

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
			songProgressBar.findView().getProgressDrawable().setColorFilter(getResources().getColor(R.color.custom_transparent_white), PorterDuff.Mode.SRC_IN);
	}
	
	@Override
	public void onStart() {
		super.onStart();

		updateKeepScreenOnStatus();

		InstantiateSessionConnectionActivity.restoreSessionConnection(this)
			.eventually(LoopedInPromise.response(new VoidResponse<>(restore -> {
				if (!restore) initializeView();
			}), messageHandler.getObject()));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == InstantiateSessionConnectionActivity.ACTIVITY_ID) initializeView();

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setNowPlayingBackgroundBitmap() {
		if (nowPlayingBackgroundBitmap != null) {
			final ImageView nowPlayingImageLoadingView = nowPlayingImageLoading.findView();
			nowPlayingImageLoadingView.setImageBitmap(nowPlayingBackgroundBitmap);
			nowPlayingImageLoadingView.setScaleType(ScaleType.CENTER_CROP);
			return;
		}

		lazyDefaultBitmap.getObject()
			.eventually(bitmap -> new LoopedInPromise<>(() -> {
				nowPlayingBackgroundBitmap = bitmap;

				setNowPlayingBackgroundBitmap();

				return null;
			}, messageHandler.getObject()));
	}

	private void initializeView() {
		playButton.findView().setVisibility(View.VISIBLE);
		pauseButton.findView().setVisibility(View.INVISIBLE);

		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(np -> SessionConnection.getInstance(NowPlayingActivity.this)
				.promiseSessionConnection()
				.eventually(LoopedInPromise.response(connectionProvider -> {
					final ServiceFile serviceFile = np.playlist.get(np.playlistPosition);
					final long filePosition = connectionProvider != null && viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey()))
						? viewStructure.filePosition
						: np.filePosition;

					setView(serviceFile, filePosition);
					return null;
				}, messageHandler.getObject())))
			.excuse(new VoidResponse<>(error -> logger.warn("An error occurred initializing `NowPlayingActivity`", error)));

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

	private void setRepeatingIcon(final ImageButton imageButton) {
		setRepeatingIcon(imageButton, false);
		lazyNowPlayingRepository.getObject()
			.getNowPlaying()
			.eventually(LoopedInPromise.response(result -> {
				if (result != null)
					setRepeatingIcon(imageButton, result.isRepeating);

				return null;
			}, messageHandler.getObject()));
	}
	
	private void setRepeatingIcon(final ImageButton imageButton, boolean isRepeating) {
		imageButton.setImageDrawable(
			isRepeating
				? repeatDrawable.getObject()
				: shuffleDrawable.getObject());
	}

	private void updateKeepScreenOnStatus() {
		isScreenKeptOnButton.findView().setImageDrawable(
			isScreenKeptOn
				? screenOnDrawable.getObject()
				: screenOffDrawable.getObject());

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
			.eventually(np -> SessionConnection.getInstance(this)
				.promiseSessionConnection()
				.eventually(LoopedInPromise.response(connectionProvider -> {
					if (playlistPosition >= np.playlist.size()) return null;

					final ServiceFile serviceFile = np.playlist.get(playlistPosition);

					final long filePosition =
						viewStructure != null && viewStructure.urlKeyHolder.equals(new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey()))
							? viewStructure.filePosition
							: 0;

					setView(serviceFile, filePosition);

					return null;
				}, messageHandler.getObject())))
			.excuse(new VoidResponse<>(e -> logger.error("An error occurred while getting the Now Playing data", e)));
	}
	
	private void setView(final ServiceFile serviceFile, final long initialFilePosition) {
		SessionConnection.getInstance(this).promiseSessionConnection()
			.eventually(LoopedInPromise.response(new VoidResponse<>(connectionProvider -> {
				final UrlKeyHolder<Integer> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile.getKey());

				if (viewStructure != null && !viewStructure.urlKeyHolder.equals(urlKeyHolder)) {
					viewStructure.release();
					viewStructure = null;
				}

				if (viewStructure == null)
					viewStructure = new ViewStructure(urlKeyHolder, serviceFile);

				final ViewStructure localViewStructure = viewStructure;

				setNowPlayingImage(localViewStructure, serviceFile);

				if (localViewStructure.fileProperties != null) {
					setFileProperties(serviceFile, initialFilePosition, localViewStructure.fileProperties);
					return;
				}

				disableViewWithMessage();

				final FilePropertiesProvider filePropertiesProvider =
					new FilePropertiesProvider(connectionProvider, FilePropertyCache.getInstance(), ParsingScheduler.instance());
				filePropertiesProvider.promiseFileProperties(serviceFile)
					.eventually(LoopedInPromise.response(fileProperties -> {
						if (localViewStructure != viewStructure) return null;

						localViewStructure.fileProperties = fileProperties;
						setFileProperties(serviceFile, initialFilePosition, fileProperties);
						return null;
					}, messageHandler.getObject()))
					.excuse(e -> LoopedInPromise.<Throwable, Boolean>response(exception -> handleIoException(serviceFile, initialFilePosition, exception), messageHandler.getObject()).promiseResponse(e));
			}), messageHandler.getObject()));
	}

	private void setNowPlayingImage(ViewStructure viewStructure, ServiceFile serviceFile) {
		final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();

		loadingProgressBar.findView().setVisibility(View.VISIBLE);
		nowPlayingImage.setVisibility(View.INVISIBLE);

		final Promise<Void> promiseDefaultColors = lazyDefaultPalette.getObject()
			.eventually(LoopedInPromise.response(new VoidResponse<>(this::setNowPlayingColors), messageHandler.getObject()));

		if (viewStructure.promisedNowPlayingImage == null) {
			viewStructure.promisedNowPlayingImage =
				lazyImageProvider.getObject()
					.eventually(provider -> provider
						.promiseFileBitmap(serviceFile)
						.eventually(b -> lazyMediaStyleProvider.getObject()
							.promisePalette(b)
							.then(p -> new StyleBitmapPair(p, b))));
		}

		promiseDefaultColors.eventually(v -> viewStructure.promisedNowPlayingImage)
			.eventually(LoopedInPromise.response(new VoidResponse<>(styleBitmapPair -> {
				if (viewStructure != NowPlayingActivity.viewStructure) return;

				setNowPlayingImage(styleBitmapPair.bitmap);
				setNowPlayingColors(styleBitmapPair.mediaStylePalette);
			}), messageHandler.getObject()))
			.excuse(new VoidResponse<>(e -> {
				if (e instanceof CancellationException) {
					logger.info("Bitmap retrieval cancelled", e);
					return;
				}

				logger.error("There was an error retrieving the image for serviceFile " + serviceFile, e);
			}));
	}

	private void setNowPlayingImage(Bitmap bitmap) {
		nowPlayingImageViewFinder.findView().setImageBitmap(bitmap);

		loadingProgressBar.findView().setVisibility(View.INVISIBLE);
		if (bitmap != null)
			displayImageBitmap();
	}

	private void setNowPlayingColors(MediaStylePalette mediaStylePalette) {
		final int tintColor = mediaStylePalette.getPrimaryTextColor();
		nowPlayingArtist.findView().setTextColor(tintColor);
		nowPlayingTitle.findView().setTextColor(tintColor);

		for (final Drawable drawable : tintableDrawables.getObject()) {
			DrawableCompat.setTint(drawable, tintColor);
		}

		nowPlayingLayout.findView().setBackgroundColor(ColorUtils.setAlphaComponent(mediaStylePalette.getActionBarColor(), 0x66));
	}

	private void setFileProperties(final ServiceFile serviceFile, final long initialFilePosition, Map<String, String> fileProperties) {
		final String artist = fileProperties.get(FilePropertiesProvider.ARTIST);
		nowPlayingArtist.findView().setText(artist);

		final String title = fileProperties.get(FilePropertiesProvider.NAME);
		nowPlayingTitle.findView().setText(title);
		nowPlayingTitle.findView().setSelected(true);

		final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

		setTrackDuration(duration > 0 ? duration : 100);
		setTrackProgress(initialFilePosition);

		final String stringRating = fileProperties.get(FilePropertiesProvider.RATING);
		try {
			if (stringRating != null && !stringRating.isEmpty()) {
				final float fileRating = Float.valueOf(stringRating);
				setFileRating(serviceFile, fileRating);
				return;
			}
		} catch (NumberFormatException e) {
			logger.info("Failed to parse rating", e);
		}

		setFileRating(serviceFile, 0f);
	}

	private void setFileRating(ServiceFile serviceFile, float rating) {
		final RatingBar songRatingBar = songRating.findView();
		songRatingBar.setRating(rating);

		songRatingBar.setOnRatingBarChangeListener((ratingBar, newRating, fromUser) -> {
			if (!fromUser || !nowPlayingToggledVisibilityControls.getObject().isVisible())
				return;

			final String stringRating = String.valueOf(Math.round(newRating));
			SessionConnection.getInstance(this).promiseSessionConnection()
				.then(new VoidResponse<>(c -> FilePropertiesStorage.storeFileProperty(c, FilePropertyCache.getInstance(), serviceFile, FilePropertiesProvider.RATING, stringRating, false)));
			viewStructure.fileProperties.put(FilePropertiesProvider.RATING, stringRating);
		});

		songRatingBar.setEnabled(true);
	}

	private void setTrackDuration(long duration) {
		songProgressBar.findView().setMax((int) duration);

		if (viewStructure != null)
			viewStructure.fileDuration = duration;
	}

	private void setTrackProgress(long progress) {
		songProgressBar.findView().setProgress((int)progress);

		if (viewStructure != null)
			viewStructure.filePosition = progress;
	}

	private boolean handleIoException(ServiceFile serviceFile, long position, Throwable exception) {
		if (ConnectionLostExceptionFilter.isConnectionLostException(exception)) {
			resetViewOnReconnect(serviceFile, position);
			return true;
		}

		UnexpectedExceptionToaster.announce(this, exception);

		return false;
	}
	
	private void displayImageBitmap() {
		final ImageView nowPlayingImage = nowPlayingImageViewFinder.findView();
		nowPlayingImage.setScaleType(ScaleType.CENTER_CROP);
		nowPlayingImage.setVisibility(View.VISIBLE);
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
	
	private void resetViewOnReconnect(final ServiceFile serviceFile, final long position) {
		PollConnectionService.pollSessionConnection(this).then(new VoidResponse<>(connectionProvider -> {
			if (viewStructure == null || !serviceFile.equals(viewStructure.serviceFile)) return;

			if (viewStructure.promisedNowPlayingImage != null) {
				viewStructure.promisedNowPlayingImage.cancel();
				viewStructure.promisedNowPlayingImage = null;
			}

			setView(serviceFile, position);
		}));

		WaitForConnectionDialog.show(this);
	}

	private void disableViewWithMessage() {
		nowPlayingTitle.findView().setText(R.string.lbl_loading);
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

		PollConnectionService.removeOnConnectionLostListener(onConnectionLostListener);
	}

	private static class ViewStructure {
		final UrlKeyHolder<Integer> urlKeyHolder;
		final ServiceFile serviceFile;
		Map<String, String> fileProperties;
		Promise<StyleBitmapPair> promisedNowPlayingImage;
		long filePosition;
		long fileDuration;

		ViewStructure(UrlKeyHolder<Integer> urlKeyHolder, ServiceFile serviceFile) {
			this.urlKeyHolder = urlKeyHolder;
			this.serviceFile = serviceFile;
		}

		void release() {
			if (promisedNowPlayingImage != null)
				promisedNowPlayingImage.cancel();
		}
	}

	private static class StyleBitmapPair {
		final MediaStylePalette mediaStylePalette;
		final Bitmap bitmap;

		private StyleBitmapPair(MediaStylePalette mediaStylePalette, Bitmap bitmap) {
			this.mediaStylePalette = mediaStylePalette;
			this.bitmap = bitmap;
		}
	}
}
