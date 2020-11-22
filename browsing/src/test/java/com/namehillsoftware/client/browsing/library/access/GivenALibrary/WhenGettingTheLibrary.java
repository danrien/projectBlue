package com.namehillsoftware.client.browsing.library.access.GivenALibrary;

import androidx.test.core.app.ApplicationProvider;

import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.lazyj.Lazy;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class WhenGettingTheLibrary extends AndroidContext {

	private static final Lazy<Library> expectedLibrary = new Lazy<>(() -> new Library()
		.setLibraryName("SomeName")
		.setAccessCode("aCxeS")
		.setCustomSyncedFilesPath("custom")
		.setIsSyncLocalConnectionsOnly(true)
		.setIsUsingExistingFiles(true)
		.setIsWakeOnLanEnabled(true)
		.setLocalOnly(true)
		.setNowPlayingId(14)
		.setNowPlayingProgress(80000000000000L)
		.setPassword("somePass")
		.setUserName("myUser")
		.setSelectedView(32)
		.setSelectedViewType(Library.ViewType.StandardServerView)
		.setRepeating(true)
		.setSavedTracksString("This is not even a real track string")
		.setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM));

	private static Library retrievedLibrary;

	public void before() throws ExecutionException, InterruptedException {
		final LibraryRepository libraryRepository = new LibraryRepository(ApplicationProvider.getApplicationContext());
		retrievedLibrary = new FuturePromise<>(
			libraryRepository
				.saveLibrary(expectedLibrary.getObject())
				.eventually(l -> libraryRepository.getLibrary(l.getLibraryId()))).get();
	}

	@Test
	public void thenTheLibraryIsCorrect() {
		assertThat(retrievedLibrary).isEqualTo(expectedLibrary.getObject());
	}
}
