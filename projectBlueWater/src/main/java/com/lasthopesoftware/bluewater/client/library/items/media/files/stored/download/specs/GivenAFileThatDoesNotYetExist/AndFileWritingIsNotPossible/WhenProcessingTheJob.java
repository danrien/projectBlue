package com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.specs.GivenAFileThatDoesNotYetExist.AndFileWritingIsNotPossible;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IServiceFileUriQueryParamsProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.IStoredFileAccess;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.StoredFileJob;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileJobException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileReadException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.download.exceptions.StoredFileWriteException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.storage.write.exceptions.StorageCreatePathException;
import com.lasthopesoftware.storage.write.permissions.IFileWritePossibleArbitrator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class WhenProcessingTheJob {

	private static StoredFileWriteException storedFileWriteException;

	@BeforeClass
	public static void before() throws StoredFileJobException, StorageCreatePathException, StoredFileReadException {
		final StoredFile storedFile = new StoredFile(new Library(), 1, new ServiceFile(1), "test-path", true);
		storedFile.setIsDownloadComplete(true);

		final StoredFileJob storedFileJob = new StoredFileJob(
			$ -> mock(File.class),
			mock(IConnectionProvider.class),
			mock(IStoredFileAccess.class),
			mock(IServiceFileUriQueryParamsProvider.class),
			f -> false,
			mock(IFileWritePossibleArbitrator.class),
			(is, f) -> {},
			new ServiceFile(1),
			storedFile);

		try {
			storedFileJob.processJob();
		} catch (StoredFileWriteException we) {
			storedFileWriteException = we;
		}
	}

	@Test
	public void thenAStoredFileWriteExceptionIsThrown() {
		assertThat(storedFileWriteException).isNotNull();
	}
}
