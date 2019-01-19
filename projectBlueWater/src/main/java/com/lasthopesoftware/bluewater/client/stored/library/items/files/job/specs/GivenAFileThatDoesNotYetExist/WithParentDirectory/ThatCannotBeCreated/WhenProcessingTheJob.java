package com.lasthopesoftware.bluewater.client.stored.library.items.files.job.specs.GivenAFileThatDoesNotYetExist.WithParentDirectory.ThatCannotBeCreated;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJob;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.job.StoredFileJobProcessor;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenProcessingTheJob {

	private static final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
	private static StorageCreatePathException storageCreatePathException;

	@BeforeClass
	public static void before() {
		final IConnectionProvider fakeConnectionProvider = mock(IConnectionProvider.class);

		final StoredFileJobProcessor storedFileJobProcessor = new StoredFileJobProcessor(
			$ -> {
				final File file = mock(File.class);
				final File parentFile = mock(File.class);
				when(parentFile.mkdirs()).thenReturn(false);
				when(file.getParentFile()).thenReturn(parentFile);

				return file;
			},
			fakeConnectionProvider,
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			f -> true,
			(is, f) -> {});

		try {
			storedFileJobProcessor.observeStoredFileDownload(
				new StoredFileJob(new ServiceFile(1), storedFile)).blockingSubscribe();
		} catch (Throwable e) {
			if (e.getCause() instanceof StorageCreatePathException)
				storageCreatePathException = (StorageCreatePathException)e.getCause();
		}
	}

	@Test
	public void thenAStorageCreatePathExceptionIsThrown() {
		assertThat(storageCreatePathException).isNotNull();
	}
}
