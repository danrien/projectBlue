package com.lasthopesoftware.bluewater.client.library.items.media.files.stored;

import android.content.Context;
import android.database.SQLException;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityInformation;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.IMediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.MediaQueryCursorProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.sync.LookupSyncDirectory;
import com.lasthopesoftware.bluewater.repository.CloseableTransaction;
import com.lasthopesoftware.bluewater.repository.InsertBuilder;
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper;
import com.lasthopesoftware.bluewater.repository.UpdateBuilder;
import com.lasthopesoftware.storage.read.permissions.ExternalStorageReadPermissionsArbitratorForOs;
import com.lasthopesoftware.storage.read.permissions.IStorageReadPermissionArbitratorForOs;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.Lazy;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class StoredFileAccess implements IStoredFileAccess {

	private static final Executor storedFileAccessExecutor = Executors.newSingleThreadExecutor();

	private static final Logger logger = LoggerFactory.getLogger(StoredFileAccess.class);

	private final Context context;
	private final LookupSyncDirectory lookupSyncDirectory;
	private final GetAllStoredFilesInLibrary getAllStoredFilesInLibrary;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	private static final String selectFromStoredFiles = "SELECT * FROM " + StoredFileEntityInformation.tableName;

	private static final Lazy<String> insertSql
			= new Lazy<>(() ->
				InsertBuilder
						.fromTable(StoredFileEntityInformation.tableName)
						.addColumn(StoredFileEntityInformation.serviceIdColumnName)
						.addColumn(StoredFileEntityInformation.libraryIdColumnName)
						.addColumn(StoredFileEntityInformation.isOwnerColumnName)
						.build());

	private static final Lazy<String> updateSql =
			new Lazy<>(() ->
					UpdateBuilder
							.fromTable(StoredFileEntityInformation.tableName)
							.addSetter(StoredFileEntityInformation.serviceIdColumnName)
							.addSetter(StoredFileEntityInformation.storedMediaIdColumnName)
							.addSetter(StoredFileEntityInformation.pathColumnName)
							.addSetter(StoredFileEntityInformation.isOwnerColumnName)
							.addSetter(StoredFileEntityInformation.isDownloadCompleteColumnName)
							.setFilter("WHERE id = @id")
							.buildQuery());

	private static final Lazy<Pattern> reservedCharactersPattern = new Lazy<>(() -> Pattern.compile("[|?*<\":>+\\[\\]'/]"));

	public StoredFileAccess(
		Context context,
		LookupSyncDirectory lookupSyncDirectory,
		GetAllStoredFilesInLibrary getAllStoredFilesInLibrary,
		CachedFilePropertiesProvider cachedFilePropertiesProvider) {

		this.context = context;
		this.lookupSyncDirectory = lookupSyncDirectory;
		this.getAllStoredFilesInLibrary = getAllStoredFilesInLibrary;
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
	}

	@Override
	public Promise<StoredFile> getStoredFile(final int storedFileId) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(repositoryAccessHelper, storedFileId);
			}
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<StoredFile> getStoredFile(Library library, final ServiceFile serviceFile) {
		return getStoredFileTask(library, serviceFile);
	}

	private Promise<StoredFile> getStoredFileTask(Library library, final ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return getStoredFile(library, repositoryAccessHelper, serviceFile);
			}
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<List<StoredFile>> getDownloadingStoredFiles() {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				return repositoryAccessHelper
					.mapSql(
						selectFromStoredFiles + " WHERE " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = @" + StoredFileEntityInformation.isDownloadCompleteColumnName)
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, false)
					.fetch(StoredFile.class);
			}
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<StoredFile> markStoredFileAsDownloaded(final StoredFile storedFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {

					repositoryAccessHelper
							.mapSql(
								" UPDATE " + StoredFileEntityInformation.tableName +
								" SET " + StoredFileEntityInformation.isDownloadCompleteColumnName + " = 1" +
								" WHERE id = @id")
							.addParameter("id", storedFile.getId())
							.execute();

					closeableTransaction.setTransactionSuccessful();
				}
			}

			storedFile.setIsDownloadComplete(true);
			return storedFile;
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<Void> addMediaFile(Library library, final ServiceFile serviceFile, final int mediaFileId, final String filePath) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				StoredFile storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile);

				if (storedFile == null) {
					createStoredFile(library, repositoryAccessHelper, serviceFile);
					storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile)
						.setIsOwner(false)
						.setIsDownloadComplete(true)
						.setPath(filePath);
				}

				storedFile.setStoredMediaId(mediaFileId);
				updateStoredFile(repositoryAccessHelper, storedFile);

				return null;
			}
		}, storedFileAccessExecutor);
	}

	@Override
	public Promise<StoredFile> promiseStoredFileUpsert(Library library, final ServiceFile serviceFile) {
		return new QueuedPromise<>(() -> {
			try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				final StoredFile storedFile = getStoredFile(library, repositoryAccessHelper, serviceFile);
				if (storedFile != null) return storedFile;

				logger.info("Stored serviceFile was not found for " + serviceFile.getKey() + ", creating serviceFile");
				createStoredFile(library, repositoryAccessHelper, serviceFile);
				return getStoredFile(library, repositoryAccessHelper, serviceFile);
			}
		}, storedFileAccessExecutor)
		.eventually(storedFile -> {
			if (storedFile.getPath() != null || !library.isUsingExistingFiles())
				return new Promise<>(storedFile);

			final IStorageReadPermissionArbitratorForOs externalStorageReadPermissionsArbitrator = new ExternalStorageReadPermissionsArbitratorForOs(context);
			final IMediaQueryCursorProvider mediaQueryCursorProvider = new MediaQueryCursorProvider(context, cachedFilePropertiesProvider);

			final MediaFileUriProvider mediaFileUriProvider =
				new MediaFileUriProvider(context, mediaQueryCursorProvider, externalStorageReadPermissionsArbitrator, library, true);

			return mediaFileUriProvider
				.promiseFileUri(serviceFile)
				.eventually(localUri -> {
					if (localUri == null)
						return new Promise<>(storedFile);

					storedFile.setPath(localUri.getPath());
					storedFile.setIsDownloadComplete(true);
					storedFile.setIsOwner(false);
					try {
						final MediaFileIdProvider mediaFileIdProvider = new MediaFileIdProvider(mediaQueryCursorProvider, serviceFile, externalStorageReadPermissionsArbitrator);
						return
							mediaFileIdProvider
								.getMediaId()
								.then(mediaId -> {
									storedFile.setStoredMediaId(mediaId);
									return storedFile;
								});
					} catch (IOException e) {
						logger.error("Error retrieving media serviceFile ID", e);
						return new Promise<>(storedFile);
					}
				});
			})
			.eventually(storedFile -> {
				if (storedFile.getPath() != null)
					return new Promise<>(storedFile);

				return cachedFilePropertiesProvider
					.promiseFileProperties(serviceFile)
					.eventually(fileProperties -> lookupSyncDirectory.promiseSyncDrive(library)
						.then(syncDrive -> {
							String fullPath = syncDrive.getPath();

							String artist = fileProperties.get(FilePropertiesProvider.ALBUM_ARTIST);
							if (artist == null)
								artist = fileProperties.get(FilePropertiesProvider.ARTIST);

							if (artist != null)
								fullPath = FilenameUtils.concat(fullPath, replaceReservedCharsAndPath(artist.trim()));

							final String album = fileProperties.get(FilePropertiesProvider.ALBUM);
							if (album != null)
								fullPath = FilenameUtils.concat(fullPath, replaceReservedCharsAndPath(album.trim()));

							String fileName = fileProperties.get(FilePropertiesProvider.FILENAME);
							fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);

							final int extensionIndex = fileName.lastIndexOf('.');
							if (extensionIndex > -1)
								fileName = fileName.substring(0, extensionIndex + 1) + "mp3";

							fullPath = FilenameUtils.concat(fullPath, fileName).trim();

							storedFile.setPath(fullPath);

							return storedFile;
						}));
			})
			.eventually(storedFile -> new QueuedPromise<>(() -> {
				try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
					updateStoredFile(repositoryAccessHelper, storedFile);
					return storedFile;
				}
			}, storedFileAccessExecutor));
	}

	private static String replaceReservedCharsAndPath(String path) {
		return reservedCharactersPattern.getObject().matcher(path).replaceAll("_");
	}

	@Override
	public Promise<Void> pruneStoredFiles(Library library, final Set<ServiceFile> serviceFilesToKeep) {
		return getAllStoredFilesInLibrary.promiseAllStoredFiles(library)
			.eventually(new PruneFilesTask(this, serviceFilesToKeep));
	}

	private StoredFile getStoredFile(Library library, RepositoryAccessHelper helper, ServiceFile serviceFile) {
		return
			helper
				.mapSql(
					" SELECT * " +
					" FROM " + StoredFileEntityInformation.tableName + " " +
					" WHERE " + StoredFileEntityInformation.serviceIdColumnName + " = @" + StoredFileEntityInformation.serviceIdColumnName +
					" AND " + StoredFileEntityInformation.libraryIdColumnName + " = @" + StoredFileEntityInformation.libraryIdColumnName)
				.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.getKey())
				.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
				.fetchFirst(StoredFile.class);
	}

	private StoredFile getStoredFile(RepositoryAccessHelper helper, int storedFileId) {
		return
			helper
				.mapSql("SELECT * FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
				.addParameter("id", storedFileId)
				.fetchFirst(StoredFile.class);
	}

	private void createStoredFile(Library library, RepositoryAccessHelper repositoryAccessHelper, ServiceFile serviceFile) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(insertSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, serviceFile.getKey())
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, library.getId())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, true)
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}

	private static void updateStoredFile(RepositoryAccessHelper repositoryAccessHelper, StoredFile storedFile) {
		try (CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {
			repositoryAccessHelper
					.mapSql(updateSql.getObject())
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, storedFile.getServiceId())
					.addParameter(StoredFileEntityInformation.storedMediaIdColumnName, storedFile.getStoredMediaId())
					.addParameter(StoredFileEntityInformation.pathColumnName, storedFile.getPath())
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, storedFile.isOwner())
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, storedFile.isDownloadComplete())
					.addParameter("id", storedFile.getId())
					.execute();

			closeableTransaction.setTransactionSuccessful();
		}
	}

	void deleteStoredFile(final StoredFile storedFile) {
		storedFileAccessExecutor.execute(() -> {
			try (final RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
				try (final CloseableTransaction closeableTransaction = repositoryAccessHelper.beginTransaction()) {

					repositoryAccessHelper
						.mapSql("DELETE FROM " + StoredFileEntityInformation.tableName + " WHERE id = @id")
						.addParameter("id", storedFile.getId())
						.execute();

					closeableTransaction.setTransactionSuccessful();
				} catch (SQLException e) {
					logger.error("There was an error deleting serviceFile " + storedFile.getId(), e);
				}
			}
		});
	}
}
