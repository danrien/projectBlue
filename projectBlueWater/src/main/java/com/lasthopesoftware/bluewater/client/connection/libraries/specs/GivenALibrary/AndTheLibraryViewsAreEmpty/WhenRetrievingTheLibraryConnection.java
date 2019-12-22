package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary.AndTheLibraryViewsAreEmpty;

import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheLibraryConnection {

	private static final IUrlProvider urlProvider = mock(IUrlProvider.class);
	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static IConnectionProvider connectionProvider;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);
		when(libraryProvider.getLibrary(2)).thenReturn(libraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(urlProvider));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			Promise::new,
			liveUrlProvider,
			(provider) -> new Promise<>(Collections.emptyList()),
			OkHttpFactory.getInstance());

		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(2))
			.updates(statuses::add));

		libraryDeferredPromise.resolve();

		connectionProvider = futureConnectionProvider.get();
	}

	@Test
	public void thenAConnectionProviderIsNotReturned() {
		assertThat(connectionProvider).isNull();
	}

	@Test
	public void thenGettingViewFailedIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.GettingView,
				BuildingConnectionStatus.GettingViewFailed);
	}
}
