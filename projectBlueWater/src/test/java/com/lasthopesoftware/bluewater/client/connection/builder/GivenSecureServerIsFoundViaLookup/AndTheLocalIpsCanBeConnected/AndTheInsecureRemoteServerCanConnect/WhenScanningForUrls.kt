package com.lasthopesoftware.bluewater.client.connection.builder.GivenSecureServerIsFoundViaLookup.AndTheLocalIpsCanBeConnected.AndTheInsecureRemoteServerCanConnect

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test

class WhenScanningForUrls {
	@Test
	fun thenTheInsecureUrlProviderIsReturned() {
		Assertions.assertThat(urlProvider).isNotNull
	}

	@Test
	fun thenTheBaseUrlIsCorrect() {
		Assertions.assertThat(urlProvider!!.baseUrl).isEqualTo("http://1.2.3.4:143/MCWS/v1/")
	}

	companion object {
		private var urlProvider: IUrlProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionTester = mockk<TestConnections>()
			every { connectionTester.promiseIsConnectionPossible(any()) } returns false.toPromise()
			every { connectionTester.promiseIsConnectionPossible(match { a -> listOf(
				"https://192.168.1.56:452/MCWS/v1/",
				"http://1.2.3.4:143/MCWS/v1/").contains(a.urlProvider.baseUrl) }) } returns true.toPromise()

			val serverLookup = mockk<LookupServers>()
			every { serverLookup.promiseServerInformation(LibraryId(15)) } returns Promise(
				ServerInfo(
					143,
					452,
					"1.2.3.4",
					listOf(
						"53.24.19.245",
						"192.168.1.56"
					), emptyList(),
					"2386166660562C5AAA1253B2BED7C2483F9C2D45"
				)
			)

			val connectionSettingsLookup = mockk<ConnectionSettingsLookup>()
			every { connectionSettingsLookup.lookupConnectionSettings(LibraryId(777)) } returns ConnectionSettings(accessCode = "gooPc").toPromise()

			val urlScanner = UrlScanner(
				mockk(),
				connectionTester,
				serverLookup,
				connectionSettingsLookup,
				OkHttpFactory.getInstance()
			)

			urlProvider = urlScanner.promiseBuiltUrlProvider(LibraryId(777)).toFuture().get()
		}
	}
}
