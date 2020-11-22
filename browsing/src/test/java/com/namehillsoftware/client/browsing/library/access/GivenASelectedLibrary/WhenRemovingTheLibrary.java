package com.namehillsoftware.client.browsing.library.access.GivenASelectedLibrary;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval;
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.specs.FakeStoredItemAccess;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.client.browsing.library.access.FakeLibraryProvider;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRemovingTheLibrary {

	private static LibraryId selectedLibraryId;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Library library = new Library();
		library.setId(14);

		final IStoredItemAccess fakeStoredItemAccess = new FakeStoredItemAccess(
			new StoredItem(14, 1, StoredItem.ItemType.ITEM),
			new StoredItem(1, 3, StoredItem.ItemType.ITEM),
			new StoredItem(5, 2, StoredItem.ItemType.ITEM),
			new StoredItem(14, 5, StoredItem.ItemType.ITEM));

		final ILibraryStorage libraryStorage = mock(ILibraryStorage.class);
		when(libraryStorage.removeLibrary(library)).thenReturn(Promise.empty());

		final ISelectedLibraryIdentifierProvider libraryIdentifierProvider = library::getLibraryId;

		final FakeLibraryProvider libraryProvider = new FakeLibraryProvider(
			library,
			new Library().setId(4),
			new Library().setId(15));

		final LibraryRemoval libraryRemoval = new LibraryRemoval(
			fakeStoredItemAccess,
			libraryStorage,
			libraryIdentifierProvider,
			libraryProvider,
			libraryId -> libraryProvider.getLibrary(libraryId).then(l -> {
				selectedLibraryId = l.getLibraryId();
				return l;
			}));

		new FuturePromise<>(libraryRemoval.removeLibrary(library)).get();
	}

	@Test
	public void thenTheFirstUnRemovedLibraryIsSelected() {
		assertThat(selectedLibraryId).isEqualTo(new LibraryId(4));
	}
}
