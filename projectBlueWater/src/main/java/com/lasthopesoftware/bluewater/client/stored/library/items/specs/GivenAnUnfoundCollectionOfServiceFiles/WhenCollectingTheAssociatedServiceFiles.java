package com.lasthopesoftware.bluewater.client.stored.library.items.specs.GivenAnUnfoundCollectionOfServiceFiles;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.IItem;
import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.ProvideLibraryFiles;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.parameters.FileListParameters;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.stored.library.items.IStoredItemAccess;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.annimon.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenCollectingTheAssociatedServiceFiles {

	private static Collection<ServiceFile> collectedFiles;
	private static List<ServiceFile> firstItemExpectedFiles = givenARandomCollectionOfFiles();
	private static List<ServiceFile> thirdItemExpectedFiles = givenARandomCollectionOfFiles();
	private static HashMap<IItem, Boolean> syncToggledItems = new HashMap<>();

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {

		final IStoredItemAccess storedItemAccess =
			new IStoredItemAccess() {
				@Override
				public void toggleSync(LibraryId libraryId, IItem item, boolean enable) {
					syncToggledItems.put(item, enable);
				}

				@Override
				public Promise<Boolean> isItemMarkedForSync(LibraryId libraryId, IItem item) {
					return null;
				}

				@Override
				public Promise<Collection<StoredItem>> promiseStoredItems(LibraryId libraryId) {
					return new Promise<>(Arrays.asList(
						new StoredItem(1, 1, StoredItem.ItemType.ITEM),
						new StoredItem(1, 2, StoredItem.ItemType.ITEM),
						new StoredItem(1, 3, StoredItem.ItemType.ITEM)));
				}
			};

		final FileListParameters fileListParameters = FileListParameters.getInstance();

		final ProvideLibraryFiles fileProvider = mock(ProvideLibraryFiles.class);
		when(fileProvider.promiseFiles(new LibraryId(4), FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(1))))
			.thenAnswer(e -> new Promise<>(firstItemExpectedFiles));
		when(fileProvider.promiseFiles(new LibraryId(4), FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(2))))
			.thenAnswer(e -> new Promise<>(new FileNotFoundException()));
		when(fileProvider.promiseFiles(new LibraryId(4), FileListParameters.Options.None, fileListParameters.getFileListParameters(new Item(3))))
			.thenAnswer(e -> new Promise<>(thirdItemExpectedFiles));

		final StoredItemServiceFileCollector serviceFileCollector = new StoredItemServiceFileCollector(
			storedItemAccess,
			fileProvider,
			fileListParameters);

		collectedFiles =
			new FuturePromise<>(
				serviceFileCollector.promiseServiceFilesToSync(new LibraryId(4)))
			.get(1000, TimeUnit.SECONDS);
	}

	@Test
	public void thenOnlyTheFoundServiceFilesAreReturned() {
		assertThat(collectedFiles).hasSameElementsAs(new HashSet<>(concat(Stream.of(firstItemExpectedFiles), Stream.of(thirdItemExpectedFiles)).toList()));
	}

	@Test
	public void thenTheFileThatWasNotFoundHadSyncToggledOff() {
		assertThat(syncToggledItems).hasEntrySatisfying(new Condition<IItem>() {

			@Override
			public boolean matches(IItem value) {
				return value.getKey() == 2;
			}
		}, new Condition<Boolean>() {
			@Override
			public boolean matches(Boolean value) {
				return !value;
			}
		});
	}

	private static List<ServiceFile> givenARandomCollectionOfFiles() {
		final Random random = new Random();
		final int floor = random.nextInt(10000);
		final int ceiling = random.nextInt(10000 - floor) + floor;
		return Stream.range(floor, ceiling).map(ServiceFile::new).toList();
	}
}
