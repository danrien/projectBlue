package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.connection.waking.NoopServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import kotlin.Unit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheLibraryConnectionTwice {

	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static IConnectionProvider connectionProvider;
	private static IConnectionProvider secondConnectionProvider;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		when(libraryProvider.getLibrary(new LibraryId(2))).thenReturn(libraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(firstUrlProvider));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			new NoopServerAlarm(),
			liveUrlProvider,
			mock(TestConnections.class),
			OkHttpFactory.getInstance());

		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(2))
			.updates(s -> {
				statuses.add(s);
				return Unit.INSTANCE;
			}));

		final FuturePromise<IConnectionProvider> secondFutureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(2))
			.updates(s -> {
				statuses.add(s);
				return Unit.INSTANCE;
			}));

		libraryDeferredPromise.resolve();

		connectionProvider = futureConnectionProvider.get();
		secondConnectionProvider = secondFutureConnectionProvider.get();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete);
	}
}
