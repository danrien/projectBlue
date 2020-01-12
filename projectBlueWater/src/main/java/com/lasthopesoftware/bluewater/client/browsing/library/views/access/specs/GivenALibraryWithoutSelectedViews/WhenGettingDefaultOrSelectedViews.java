package com.lasthopesoftware.bluewater.client.browsing.library.views.access.specs.GivenALibraryWithoutSelectedViews;

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

	private static StandardViewItem expectedView = new StandardViewItem(2, null);
	private static ViewItem selectedLibraryView;
	private static Library savedLibrary;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library()),
				() -> new Promise<>(
					Arrays.asList(
						new StandardViewItem(2, null),
						new StandardViewItem(1, null),
						new StandardViewItem(14, null))),
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
	public void thenTheSelectedViewKeyIsSaved() {
		assertThat(savedLibrary.getSelectedView()).isEqualTo(expectedView.getKey());
	}

	@Test
	public void thenTheSelectedViewTypeIsStandard() {
		assertThat(savedLibrary.getSelectedViewType()).isEqualTo(Library.ViewType.StandardServerView);
	}
}
