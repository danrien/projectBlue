package com.lasthopesoftware.bluewater.client.connection.session.GivenASelectedLibrary.AndGettingTheLibraryFaults

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ISelectedLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibrary
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection.BuildingSessionConnectionStatus.GettingLibraryFailed
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnectionReservation
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.resources.FakeMessageSender
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class WhenRetrievingTheSessionConnection : AndroidContext() {

	companion object {
		private val fakeMessageSender = FakeMessageSender()
		private var connectionProvider: IConnectionProvider? = null
		private var exception: IOException? = null
	}

	override fun before() {
		val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

		val libraryConnections = mockk<ProvideLibraryConnections>()
		every { libraryConnections.promiseLibraryConnection(LibraryId(2)) } returns deferredConnectionProvider

		val libraryIdentifierProvider = mockk<ISelectedLibraryIdentifierProvider>()
		every { libraryIdentifierProvider.selectedLibraryId } returns LibraryId(2)

		SessionConnectionReservation().use {
			val sessionConnection = SessionConnection(
				fakeMessageSender,
				libraryIdentifierProvider,
				libraryConnections)
			val futureConnectionProvider = FuturePromise(sessionConnection.promiseSessionConnection())
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibrary)
			deferredConnectionProvider.sendProgressUpdate(BuildingConnectionStatus.GettingLibraryFailed)
			deferredConnectionProvider.sendRejection(IOException("OMG"))
			try {
				connectionProvider = futureConnectionProvider.get()
			} catch (e: ExecutionException) {
				if (e.cause is IOException) exception = e.cause as IOException?
			}
		}
	}

	@Test
	fun thenAConnectionProviderIsNotReturned() {
		AssertionsForClassTypes.assertThat(connectionProvider).isNull()
	}

	@Test
	fun thenAnIOExceptionIsReturned() {
		AssertionsForClassTypes.assertThat(exception).isNotNull
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(fakeMessageSender.recordedIntents
			.map { i -> i.getIntExtra(SessionConnection.buildSessionBroadcastStatus, -1) }
			.toList())
			.containsExactly(GettingLibrary, GettingLibraryFailed)
	}
}
