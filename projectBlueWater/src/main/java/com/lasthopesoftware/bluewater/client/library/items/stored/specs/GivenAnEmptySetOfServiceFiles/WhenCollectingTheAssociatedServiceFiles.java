package com.lasthopesoftware.bluewater.client.library.items.stored.specs.GivenAnEmptySetOfServiceFiles;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.IFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.items.stored.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.client.library.items.stored.conversion.ConvertStoredPlaylistsToStoredItems;
import com.namehillsoftware.handoff.promises.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static com.annimon.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;
	private static List<ServiceFile> firstItemExpectedFiles = Collections.emptyList();
	private static List<ServiceFile> secondItemExpectedFiles = Collections.emptyList();
	private static List<ServiceFile> thirdItemExpectedFiles = Collections.emptyList();

	@BeforeClass
	public static void before() {

		final IStoredItemAccess storedItemAccess =
			new IStoredItemAccess() {
				@Override
				public void toggleSync(IItem item, boolean enable) {
				}

				@Override
				public Promise<Boolean> isItemMarkedForSync(IItem item) {
					return null;
				}

				@Override
				public Promise<Collection<StoredItem>> promiseStoredItems() {
					return new Promise<>(Arrays.asList(
						new StoredItem(1, 1, StoredItem.ItemType.ITEM),
						new StoredItem(1, 2, StoredItem.ItemType.ITEM),
						new StoredItem(1, 3, StoredItem.ItemType.ITEM)));
				}
			};

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final IFileProvider fileProvider = mock(IFileProvider.class);
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(1))))
			.thenAnswer(e -> new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(2))))
			.thenAnswer(e -> new Promise<>(secondItemExpectedFiles));
		when(fileProvider.promiseFiles(FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(3))))
			.thenAnswer(e -> new Promise<>(thirdItemExpectedFiles));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			mock(ConvertStoredPlaylistsToStoredItems.class),
			fileProvider);

		collectedFiles = serviceFileCollector
			.streamServiceFilesToSync().toList().blockingGet();
	}

	@Test
	public void thenOnlyTheFoundServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(new HashSet<>(concat(Stream.of(firstItemExpectedFiles), Stream.of(thirdItemExpectedFiles)).toList()));
	}
}
