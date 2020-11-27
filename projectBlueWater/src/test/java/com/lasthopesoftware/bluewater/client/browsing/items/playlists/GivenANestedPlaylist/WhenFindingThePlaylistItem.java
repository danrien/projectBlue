package com.lasthopesoftware.bluewater.client.browsing.items.playlists.GivenANestedPlaylist;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist;
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistItemFinder;
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenFindingThePlaylistItem {

	private static Item expectedItem;
	private static Item item;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Random random = new Random();
		final int playlistId = random.nextInt();

		expectedItem = new Item(random.nextInt()).setPlaylistId(playlistId);

		final ProvideLibraryViews libraryViews = mock(ProvideLibraryViews.class);
		when(libraryViews.promiseLibraryViews())
			.thenReturn(new Promise<>(Arrays.asList(
				new StandardViewItem(2, "Music"),
				new PlaylistViewItem(16))));

		final ProvideItems itemProvider = mock(ProvideItems.class);
		when(itemProvider.promiseItems(anyInt()))
			.thenReturn(new Promise<>(Collections.emptyList()));

		setupItemProviderWithItems(
			itemProvider,
			random,
			2,
			3,
			false);

		List<Item> generatedItems = setupItemProviderWithItems(
			itemProvider,
			random,
			16,
			15,
			true);

		final Item firstLevelChosenItem = generatedItems.get(random.nextInt(generatedItems.size()));

		for (Item item : generatedItems) {
			if (item.equals(firstLevelChosenItem)) continue;

			setupItemProviderWithItems(
				itemProvider,
				random,
				item.getKey(),
				100,
				true);
		}

		generatedItems = setupItemProviderWithItems(
			itemProvider,
			random,
			16,
			90,
			true);

		final Item secondLevelChosenItem = generatedItems.get(random.nextInt(generatedItems.size()));

		for (Item item : generatedItems) {
			if (item.equals(secondLevelChosenItem)) continue;

			when(itemProvider.promiseItems(item.getKey()))
				.thenReturn(new Promise<>(Collections.emptyList()));
		}

		final Item decoy = new Item(random.nextInt()).setPlaylistId(random.nextInt());

		when(itemProvider.promiseItems(secondLevelChosenItem.getKey()))
			.thenReturn(new Promise<>(Arrays.asList(
				decoy,
				expectedItem)));

		final PlaylistItemFinder playlistItemFinder = new PlaylistItemFinder(
			libraryViews,
			itemProvider);

		item = new FuturePromise<>(playlistItemFinder.promiseItem(new Playlist(playlistId))).get();
	}

	@Test
	public void thenTheReturnedItemIsTheExpectedItem() {
		assertThat(item).isEqualTo(expectedItem);
	}

	private static List<Item> setupItemProviderWithItems(ProvideItems itemProvider, Random random, int sourceItem, int numberOfChildren, boolean withPlaylistIds) {
		final List<Item> items = new ArrayList<>(numberOfChildren);

		for (int i = 0; i < numberOfChildren; ++i) {
			final Item newItem = new Item(random.nextInt());
			if (withPlaylistIds)
				newItem.setPlaylistId(random.nextInt());
			items.add(newItem);
		}

		when(itemProvider.promiseItems(sourceItem))
			.thenReturn(new Promise<>(items));

		return items;
	}
}
