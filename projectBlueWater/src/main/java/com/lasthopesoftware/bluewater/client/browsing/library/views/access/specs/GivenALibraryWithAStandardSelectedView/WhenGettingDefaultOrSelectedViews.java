package com.lasthopesoftware.bluewater.client.browsing.library.views.access.specs.GivenALibraryWithAStandardSelectedView;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static StandardViewItem expectedView = new StandardViewItem(5, null);
	private static ViewItem selectedLibraryView;
	private static Library savedLibrary;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library().setSelectedView(5)),
				() -> new Promise<>(
					Arrays.asList(
						new StandardViewItem(3, null),
						new StandardViewItem(5, null),
						new StandardViewItem(8, null))),
				library -> {
					savedLibrary = library;
					return new Promise<>(library);
				});
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView);
	}

	@Test
	public void thenTheLibraryIsNotSaved() {
		assertThat(savedLibrary).isNull();
	}
}
