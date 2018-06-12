package com.lasthopesoftware.bluewater.client.library.sync;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.directories.GetPrivateDrives;
import com.lasthopesoftware.storage.directories.GetPublicDrives;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.File;

public class SyncDriveLookup implements LookupSyncDrive {
	private final GetPublicDrives publicDrives;
	private final GetPrivateDrives privateDrives;

	public SyncDriveLookup(GetPublicDrives publicDrives, GetPrivateDrives privateDrives) {
		this.publicDrives = publicDrives;
		this.privateDrives = privateDrives;
	}

	@Override
	public Promise<File> promiseSyncDrive(Library library) {
		return getExternalFilesDirectoriesStream(library)
			.then(files ->
				files.sortBy(File::getFreeSpace)
				.findLast()
				.orElse(null));
	}

	private Promise<Stream<File>> getExternalFilesDirectoriesStream(Library library) {
		switch (library.getSyncedFileLocation()) {
			case EXTERNAL:
				return publicDrives.promisePublicDrives();
			case INTERNAL:
				final Promise<Stream<File>> promisedPrivateDrives = privateDrives.promisePrivateDrives();
				if (library.getId() < 0) return promisedPrivateDrives;

				final String libraryId = String.valueOf(library.getId());
				return promisedPrivateDrives
					.then(files -> files.map(f -> new File(f, libraryId)));
		}

		return new Promise<>(Stream.empty());
	}
}
