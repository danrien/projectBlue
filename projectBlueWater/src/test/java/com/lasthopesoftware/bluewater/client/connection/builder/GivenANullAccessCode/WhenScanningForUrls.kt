package com.lasthopesoftware.bluewater.client.connection.builder.GivenANullAccessCode

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenScanningForUrls {

	companion object {
		private var illegalArgumentException: IllegalArgumentException? = null
		private var urlProvider: IUrlProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionSettingsLookup = mockk<LookupConnectionSettings>()
			every { connectionSettingsLookup.lookupConnectionSettings(any()) } returns Promise.empty()

			val urlScanner = UrlScanner(
				mockk(),
				mockk(),
				mockk(),
				connectionSettingsLookup,
				mockk()
			)
			try {
				urlProvider = urlScanner.promiseBuiltUrlProvider(LibraryId(32)).toFuture().get()
			} catch (e: ExecutionException) {
				if (e.cause is IllegalArgumentException) illegalArgumentException =
					e.cause as IllegalArgumentException? else throw e
			}
		}
	}

	@Test
	fun thenANullUrlProviderIsReturned() {
		assertThat(urlProvider).isNull()
	}

	@Test
	fun thenAnIllegalArgumentExceptionIsThrown() {
		assertThat(illegalArgumentException).isNotNull
	}

	@Test
	fun thenTheExceptionMentionsTheLibrary() {
		assertThat(illegalArgumentException?.message).isEqualTo("The access code cannot be null")
	}
}
