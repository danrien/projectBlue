package com.lasthopesoftware.bluewater.client.connection.libraries.specs.GivenALibrary;

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
import com.lasthopesoftware.bluewater.client.connection.waking.specs.NoopServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import kotlin.Unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheLibraryConnection {

	private static final IUrlProvider urlProvider = mock(IUrlProvider.class);
	private static final List<BuildingConnectionStatus> statuses = new ArrayList<>();
	private static IConnectionProvider connectionProvider;
	private static boolean isActiveBeforeGettingConnection;
	private static boolean isActiveAfterGettingConnection;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final Library library = new Library()
			.setId(3)
			.setAccessCode("aB5nf");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		final DeferredPromise<Library> libraryDeferredPromise = new DeferredPromise<>(library);
		when(libraryProvider.getLibrary(new LibraryId(3))).thenReturn(libraryDeferredPromise);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(urlProvider));

		final LibraryConnectionProvider libraryConnectionProvider = new LibraryConnectionProvider(
			libraryProvider,
			new NoopServerAlarm(),
			liveUrlProvider,
			mock(TestConnections.class),
			OkHttpFactory.getInstance());

		final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(libraryConnectionProvider
			.promiseLibraryConnection(new LibraryId(3))
			.updates(s -> {
				statuses.add(s);
				return Unit.INSTANCE;
			}));

		isActiveBeforeGettingConnection = libraryConnectionProvider.isConnectionActive(new LibraryId(3));

		libraryDeferredPromise.resolve();

		connectionProvider = futureConnectionProvider.get();

		isActiveAfterGettingConnection = libraryConnectionProvider.isConnectionActive(new LibraryId(3));
	}

	@Test
	public void thenTheConnectionIsNotActiveBeforeGettingConnection() {
		assertThat(isActiveBeforeGettingConnection).isFalse();
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(urlProvider);
	}

	@Test
	public void thenTheConnectionIsActiveAfterGettingConnection() {
		assertThat(isActiveAfterGettingConnection).isTrue();
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete);
	}
}
