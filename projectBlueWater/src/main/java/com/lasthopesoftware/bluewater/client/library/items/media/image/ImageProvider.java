package com.lasthopesoftware.bluewater.client.library.items.media.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.DiskFileCache;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.RejectingCancellationHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.VoidFunc;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageProvider extends QueuedPromise<Bitmap> {
	
	private static final String IMAGE_FORMAT = "jpg";
	
	private static final ExecutorService imageAccessExecutor = Executors.newSingleThreadExecutor();
	
	private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);

	private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 100 * 1024 * 1024 for 100MB of cache
	private static final int MAX_MEMORY_CACHE_SIZE = 10;
	private static final int MAX_DAYS_IN_CACHE = 30;
	private static final String IMAGES_CACHE_NAME = "images";

	private static final LruCache<String, Byte[]> imageMemoryCache = new LruCache<>(MAX_MEMORY_CACHE_SIZE);

	private static final String cancellationMessage = "The image task was cancelled";

	public static Promise<Bitmap> getImage(final Context context, IConnectionProvider connectionProvider, CachedFilePropertiesProvider cachedFilePropertiesProvider, final int fileKey) {
		return
			cachedFilePropertiesProvider
				.promiseFileProperties(fileKey)
				.then(fileProperties -> new ImageProvider(context, connectionProvider, fileProperties, fileKey));
	}

	private ImageProvider(final Context context, final IConnectionProvider connectionProvider, Map<String, String> fileProperties, final int fileKey) {
		super(new ImageMemoryTask(context, connectionProvider, new FillerBitmap(context), fileProperties, fileKey), imageAccessExecutor);
	}

	private static class ImageMemoryTask implements ThreeParameterAction<IResolvedPromise<Bitmap>, IRejectedPromise, OneParameterAction<Runnable>> {
		private final Context context;
		private final IConnectionProvider connectionProvider;
		private final Map<String, String> fileProperties;
		private final FillerBitmap fillerBitmap;
		private final int fileKey;
		private final ILibraryProvider libraryProvider;
		private final SelectedBrowserLibraryIdentifierProvider selectedLibraryIdentifierProvider;

		ImageMemoryTask(Context context, IConnectionProvider connectionProvider, FillerBitmap fillerBitmap, Map<String, String> fileProperties, int fileKey) {
			this.context = context;
			this.connectionProvider = connectionProvider;
			this.fileProperties = fileProperties;
			this.fillerBitmap = fillerBitmap;
			this.fileKey = fileKey;
			this.libraryProvider = new LibraryRepository(context);
			this.selectedLibraryIdentifierProvider = new SelectedBrowserLibraryIdentifierProvider(context);
		}

		@Override
		public void runWith(IResolvedPromise<Bitmap> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			final RejectingCancellationHandler rejectingCancellationHandler = new RejectingCancellationHandler(cancellationMessage, reject);

			onCancelled.runWith(rejectingCancellationHandler);

			if (rejectingCancellationHandler.isCancelled()) return;

			// First try storing by the album artist, which can cover the artist for the entire album (i.e. an album with various
			// artists), and next by artist if that field is empty
			String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
			if (artist == null || artist.isEmpty())
				artist = fileProperties.get(FilePropertiesProvider.ARTIST);

			String albumOrTrackName = fileProperties.get(FilePropertiesProvider.ALBUM);
			if (albumOrTrackName == null)
				albumOrTrackName = fileProperties.get(FilePropertiesProvider.NAME);

			final String uniqueKey = artist + ":" + albumOrTrackName;

			if (rejectingCancellationHandler.isCancelled()) return;

			final byte[] imageBytes = getBitmapBytesFromMemory(uniqueKey);
			if (imageBytes.length > 0) {
				resolve.sendResolution(getBitmapFromBytes(imageBytes));
				return;
			}

			libraryProvider
				.getLibrary(selectedLibraryIdentifierProvider.getSelectedLibraryId())
				.then(library -> {
					final Promise<Bitmap> httpAccessPromise =
						new QueuedPromise<>(new ImageIoAccessTask(uniqueKey, context, library, connectionProvider, fillerBitmap, fileKey), imageAccessExecutor);

					onCancelled.runWith(httpAccessPromise::cancel);

					return httpAccessPromise;
				})
				.next(VoidFunc.runCarelessly(resolve::sendResolution))
				.error(VoidFunc.runCarelessly(reject::sendRejection));
		}

		private static byte[] getBitmapBytesFromMemory(final String uniqueKey) {
			final Byte[] memoryImageBytes = imageMemoryCache.get(uniqueKey);

			if (memoryImageBytes == null) return new byte[0];

			final byte[] imageBytes = new byte[memoryImageBytes.length];
			for (int i = 0; i < memoryImageBytes.length; i++)
				imageBytes[i] = memoryImageBytes[i];

			return imageBytes;
		}
	}

	private static class ImageIoAccessTask implements ThreeParameterAction<IResolvedPromise<Bitmap>, IRejectedPromise, OneParameterAction<Runnable>> {

		private final String uniqueKey;
		private final Context context;
		private final Library library;
		private final IConnectionProvider connectionProvider;
		private final FillerBitmap fillerBitmap;
		private final int fileKey;

		ImageIoAccessTask(String uniqueKey, Context context, Library library, IConnectionProvider connectionProvider, FillerBitmap fillerBitmap, int fileKey) {
			this.uniqueKey = uniqueKey;
			this.context = context;
			this.library = library;
			this.connectionProvider = connectionProvider;
			this.fillerBitmap = fillerBitmap;
			this.fileKey = fileKey;
		}

		@Override
		public void runWith(IResolvedPromise<Bitmap> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
			if (library == null) {
				resolve.sendResolution(fillerBitmap.getFillerBitmap());
				return;
			}

			final RejectingCancellationHandler rejectingCancellationHandler = new RejectingCancellationHandler(cancellationMessage, reject);
			onCancelled.runWith(rejectingCancellationHandler);

			final DiskFileCache imageDiskCache = new DiskFileCache(context, library, IMAGES_CACHE_NAME, MAX_DAYS_IN_CACHE, MAX_DISK_CACHE_SIZE);

			byte[] imageBytes;
			try {
				final java.io.File imageCacheFile = imageDiskCache.get(uniqueKey);
				if (imageCacheFile != null) {
					imageBytes = putBitmapIntoMemory(uniqueKey, imageCacheFile);
					if (imageBytes.length > 0) {
						resolve.sendResolution(getBitmapFromBytes(imageBytes));
						return;
					}
				}
			} catch (IOException e) {
				logger.error("There was an error getting the cached serviceFile", e);
			}

			try {
				final HttpURLConnection connection = connectionProvider.getConnection("File/GetImage", "File=" + String.valueOf(fileKey), "Type=Full", "Pad=1", "Format=" + IMAGE_FORMAT, "FillTransparency=ffffff");
				try {
					// Connection failed to build
					if (connection == null) {
						resolve.sendResolution(fillerBitmap.getFillerBitmap());
						return;
					}

					try {
						if (rejectingCancellationHandler.isCancelled()) return;

						try (InputStream is = connection.getInputStream()) {
							imageBytes = IOUtils.toByteArray(is);
						} catch (InterruptedIOException interruptedIoException) {
							logger.warn("Copying the input stream to a byte array was interrupted", interruptedIoException);
							reject.sendRejection(interruptedIoException);
							return;
						}

						if (imageBytes.length == 0) {
							resolve.sendResolution(fillerBitmap.getFillerBitmap());
							return;
						}
					} catch (FileNotFoundException fe) {
						logger.warn("Image not found!");
						resolve.sendResolution(fillerBitmap.getFillerBitmap());
						return;
					}

					try {
						imageDiskCache.put(uniqueKey, imageBytes);
					} catch (IOException ioe) {
						logger.error("Error writing serviceFile!", ioe);
					}

					putBitmapIntoMemory(uniqueKey, imageBytes);

					if (rejectingCancellationHandler.isCancelled()) return;

					resolve.sendResolution(getBitmapFromBytes(imageBytes));
				} catch (Exception e) {
					logger.error(e.toString(), e);
				} finally {
					if (connection != null)
						connection.disconnect();
				}
			} catch (IOException e) {
				logger.error("There was an error getting the connection for images", e);
			}
		}

		private static void putBitmapIntoMemory(final String uniqueKey, final byte[] imageBytes) {
			final Byte[] memoryImageBytes = new Byte[imageBytes.length];

			for (int i = 0; i < imageBytes.length; i++)
				memoryImageBytes[i] = imageBytes[i];

			imageMemoryCache.put(uniqueKey, memoryImageBytes);
		}

		private static byte[] putBitmapIntoMemory(final String uniqueKey, final java.io.File file) {
			final int size = (int) file.length();
			final byte[] bytes = new byte[size];

			try {
				final FileInputStream fis = new FileInputStream(file);
				final BufferedInputStream buffer = new BufferedInputStream(fis);
				try {
					buffer.read(bytes, 0, bytes.length);
				} finally {
					buffer.close();
					fis.close();
				}
			} catch (FileNotFoundException e) {
				logger.error("Could not find serviceFile.", e);
				return new byte[0];
			} catch (IOException e) {
				logger.error("Error reading serviceFile.", e);
				return new byte[0];
			}

			putBitmapIntoMemory(uniqueKey, bytes);
			return bytes;
		}
	}

	private static class FillerBitmap {

		private static Bitmap fillerBitmap;
		private static final Object fillerBitmapSyncObj = new Object();

		private Context context;

		private FillerBitmap(Context context) {
			this.context = context;
		}

		private Bitmap getFillerBitmap() {
			synchronized (fillerBitmapSyncObj) {
				if (fillerBitmap != null) return getBitmapCopy(fillerBitmap);

				fillerBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wave_background);

				final DisplayMetrics dm = context.getResources().getDisplayMetrics();
				int maxSize = Math.max(dm.heightPixels, dm.widthPixels);

				fillerBitmap = Bitmap.createScaledBitmap(fillerBitmap, maxSize, maxSize, false);

				return getBitmapCopy(fillerBitmap);
			}
		}

		private static Bitmap getBitmapCopy(final Bitmap src) {
			return src.copy(src.getConfig(), false);
		}
	}

	private static Bitmap getBitmapFromBytes(final byte[] imageBytes) {
		return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
	}
}
