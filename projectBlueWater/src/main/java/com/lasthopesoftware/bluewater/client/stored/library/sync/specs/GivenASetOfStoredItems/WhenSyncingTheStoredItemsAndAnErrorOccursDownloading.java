package com.lasthopesoftware.bluewater.client.stored.library.sync.specs.GivenASetOfStoredItems;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileSystemFileProducer;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobState;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.sync.LibrarySyncHandler;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenSyncingTheStoredItemsAndAnErrorOccursDownloading {

	private static List<StoredFile> storedFileJobResults = new ArrayList<>();
	private static List<StoredFile> queuedStoredFiles = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final IStoredItemAccess storedItemAccessMock = mock(IStoredItemAccess.class);
		when(storedItemAccessMock.promiseStoredItems(new LibraryId(42)))
			.thenReturn(new Promise<>(Collections.singleton(
				new StoredItem(1, 14, StoredItem.ItemType.ITEM))));

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final IFileProvider mockFileProvider = mock(IFileProvider.class);
		when(mockFileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(14))))
			.thenReturn(new Promise<>(Arrays.asList(
				new ServiceFile(1),
				new ServiceFile(2),
				new ServiceFile(4),
				new ServiceFile(10))));

		final IStoredFileAccess storedFileAccess = mock(IStoredFileAccess.class);
		when(storedFileAccess.pruneStoredFiles(any(), anySet())).thenReturn(Promise.empty());

		final LibrarySyncHandler librarySyncHandler = new LibrarySyncHandler(
			new Library(),
			new StoredItemServiceFileCollector(
				storedItemAccessMock,
				mockFileProvider,
				fileListParameters),
			storedFileAccess,
			(l, f) -> new Promise<>(new StoredFile(l, 1, f, "fake-file-name", true)),
			new StoredFileJobProcessor(
				new StoredFileSystemFileProducer(),
				storedFileAccess,
				f -> f.getServiceId() == 2
					? new Promise<>(new IOException())
					: new Promise<>(new ByteArrayInputStream(new byte[0])),
				f -> true,
				f -> true,
				(i, f) -> {}));

		librarySyncHandler.observeLibrarySync()
			.filter(j -> j.storedFileJobState == StoredFileJobState.Downloaded)
			.map(j -> j.storedFile)
			.blockingSubscribe(new Observer<StoredFile>() {
				@Override
				public void onSubscribe(Disposable d) {

				}

				@Override
				public void onNext(StoredFile storedFile) {
					storedFileJobResults.add(storedFile);
				}

				@Override
				public void onError(Throwable e) {

				}

				@Override
				public void onComplete() {

				}
			});
	}

	@Test
	public void thenTheOtherFilesInTheStoredItemsAreSynced() {
		assertThat(Stream.of(storedFileJobResults).map(StoredFile::getServiceId).toList())
			.containsExactly(1, 4, 10);
	}
}
