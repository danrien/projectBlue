package com.lasthopesoftware.bluewater.client.connection.session.specs.GivenASelectedLibrary.AndGettingALiveUrlFaults.AndTheSelectedLibraryChanges;

import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.builder.live.ProvideLiveUrl;
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.connection.session.specs.SessionConnectionReservation;
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.selection.ISelectedLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.DeferredPromise;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.resources.specs.BroadcastRecorder;
import com.lasthopesoftware.resources.specs.ScopedLocalBroadcastManagerBuilder;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingConnection;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingConnectionFailed;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.BuildingSessionComplete;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary;
import static com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.buildSessionBroadcastStatus;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenRetrievingTheSessionConnectionTwice extends AndroidContext {

	private static final BroadcastRecorder broadcastRecorder = new BroadcastRecorder();
	private static final IUrlProvider firstUrlProvider = mock(IUrlProvider.class);
	private static final IUrlProvider secondUrlProvider = mock(IUrlProvider.class);
	private static IConnectionProvider connectionProvider;

	@Override
	public void before() throws ExecutionException, InterruptedException, IllegalAccessException, InstantiationException, InvocationTargetException {

		final Library library = new Library()
			.setId(2)
			.setAccessCode("aB5nf");

		final Library secondLibrary = new Library().setId(1).setAccessCode("b");

		final ILibraryProvider libraryProvider = mock(ILibraryProvider.class);
		final DeferredPromise<Library> deferredLibrary = new DeferredPromise<>(library);
		when(libraryProvider.getLibrary(2)).thenReturn(deferredLibrary);
		final DeferredPromise<Library> secondDeferredLibrary = new DeferredPromise<>(secondLibrary);
		when(libraryProvider.getLibrary(1)).thenReturn(secondDeferredLibrary);

		final ProvideLiveUrl liveUrlProvider = mock(ProvideLiveUrl.class);
		when(liveUrlProvider.promiseLiveUrl(library)).thenReturn(new Promise<>(new IOException("An error!")));
		when(liveUrlProvider.promiseLiveUrl(secondLibrary)).thenReturn(new Promise<>(secondUrlProvider));

		final FakeSelectedLibraryProvider fakeSelectedLibraryProvider = new FakeSelectedLibraryProvider();

		final LocalBroadcastManager localBroadcastManager = ScopedLocalBroadcastManagerBuilder.newScopedBroadcastManager(RuntimeEnvironment.application);
		localBroadcastManager.registerReceiver(
			broadcastRecorder,
			new IntentFilter(SessionConnection.buildSessionBroadcast));

		try (SessionConnectionReservation ignored = new SessionConnectionReservation()) {
			fakeSelectedLibraryProvider.selectedLibraryId = 2;
			final SessionConnection sessionConnection = new SessionConnection(
				localBroadcastManager,
				fakeSelectedLibraryProvider,
				new LibraryConnectionProvider(
					libraryProvider,
					liveUrlProvider,
					mock(TestConnections.class),
					OkHttpFactory.getInstance()));

			final FuturePromise<IConnectionProvider> futureConnectionProvider = new FuturePromise<>(
				sessionConnection.promiseSessionConnection()
					.eventually(
						c -> {
							fakeSelectedLibraryProvider.selectedLibraryId = 1;
							final Promise<IConnectionProvider> promise = sessionConnection.promiseSessionConnection();
							secondDeferredLibrary.resolve();
							return promise;
						},
						e -> {
							fakeSelectedLibraryProvider.selectedLibraryId = 1;
							final Promise<IConnectionProvider> promise = sessionConnection.promiseSessionConnection();
							secondDeferredLibrary.resolve();
							return promise;
						}));
			deferredLibrary.resolve();
			connectionProvider = futureConnectionProvider.get();
		}
	}

	@Test
	public void thenTheConnectionIsCorrect() {
		assertThat(connectionProvider.getUrlProvider()).isEqualTo(secondUrlProvider);
	}

	@Test
	public void thenGettingLibraryIsBroadcast() {
		Assertions.assertThat(Stream.of(broadcastRecorder.recordedIntents).map(i -> i.getIntExtra(buildSessionBroadcastStatus, -1)).toList())
			.containsExactly(
				GettingLibrary,
				BuildingConnection,
				BuildingConnectionFailed,
				GettingLibrary,
				BuildingConnection,
				BuildingSessionComplete);
	}

	private static class FakeSelectedLibraryProvider implements ISelectedLibraryIdentifierProvider {

		int selectedLibraryId;

		@Override
		public int getSelectedLibraryId() {
			return selectedLibraryId;
		}
	}
}
