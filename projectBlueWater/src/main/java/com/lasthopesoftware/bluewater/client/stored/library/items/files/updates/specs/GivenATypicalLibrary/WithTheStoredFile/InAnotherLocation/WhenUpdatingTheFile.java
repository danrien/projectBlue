package com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.specs.GivenATypicalLibrary.WithTheStoredFile.InAnotherLocation;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.specs.FakeCachedSessionFilesPropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFile;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.retrieval.StoredFileQuery;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.MediaFileIdProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.updates.StoredFileUpdater;
import com.lasthopesoftware.bluewater.client.stored.library.sync.SyncDirectoryLookup;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenUpdatingTheFile extends AndroidContext {

	private static StoredFile storedFile;

	@Override
	public void before() throws ExecutionException, InterruptedException {
		final MediaFileUriProvider mediaFileUriProvider = mock(MediaFileUriProvider.class);
		when(mediaFileUriProvider.promiseFileUri(any()))
			.thenReturn(Promise.empty());

		final MediaFileIdProvider mediaFileIdProvider = mock(MediaFileIdProvider.class);
		when(mediaFileIdProvider.getMediaId(any()))
			.thenReturn(Promise.empty());

		final FakeCachedSessionFilesPropertiesProvider filePropertiesProvider = new FakeCachedSessionFilesPropertiesProvider();
		filePropertiesProvider.addFilePropertiesToCache(
			new ServiceFile(4),
			new HashMap<String, String>() {{
				put(SessionFilePropertiesProvider.ARTIST, "artist");
				put(SessionFilePropertiesProvider.ALBUM, "album");
				put(SessionFilePropertiesProvider.FILENAME, "my-filename.mp3");
			}});

		new FuturePromise<>(new StoredFileUpdater(
			RuntimeEnvironment.application,
			mediaFileUriProvider,
			mediaFileIdProvider,
			new StoredFileQuery(RuntimeEnvironment.application),
			filePropertiesProvider,
			new SyncDirectoryLookup(
				() -> new Promise<>(Stream.of(new File("/my-public-drive-1"))),
				() -> new Promise<>(Stream.empty()))).promiseStoredFileUpdate(
			new Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL),
			new ServiceFile(4))).get();

		final StoredFileUpdater storedFileUpdater = new StoredFileUpdater(
			RuntimeEnvironment.application,
			mediaFileUriProvider,
			mediaFileIdProvider,
			new StoredFileQuery(RuntimeEnvironment.application),
			filePropertiesProvider,
			new SyncDirectoryLookup(
				() -> new Promise<>(Stream.of(new File("/my-public-drive"))),
				() -> new Promise<>(Stream.empty())));

		storedFile = new FuturePromise<>(storedFileUpdater.promiseStoredFileUpdate(
			new Library().setId(14).setSyncedFileLocation(Library.SyncedFileLocation.EXTERNAL),
			new ServiceFile(4))).get();
	}

	@Test
	public void thenTheFileIsOwnedByTheLibrary() {
		assertThat(storedFile.isOwner()).isTrue();
	}

	@Test
	public void thenTheFilePathIsCorrect() {
		assertThat(storedFile.getPath()).isEqualTo("/my-public-drive-1/14/artist/album/my-filename.mp3");
	}
}
