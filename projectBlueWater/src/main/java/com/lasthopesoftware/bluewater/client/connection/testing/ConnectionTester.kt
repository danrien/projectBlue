package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException

class ConnectionTester : TestConnections {
	override fun promiseIsConnectionPossible(connectionProvider: IConnectionProvider): Promise<Boolean> {
		return connectionProvider.promiseResponse("Alive").then({ testResponse(it) }, { false })
	}

	private fun testResponse(response: Response): Boolean {
		response.body?.use { body ->
			try {
				body.byteStream().use {
					return com.namehillsoftware.projectblue.shared.StandardRequest.fromInputStream(it)?.isStatus ?: false
				}
			} catch (e: IOException) {
				logger.error("Error closing connection, device failure?", e)
			} catch (e: IllegalArgumentException) {
				logger.warn("Illegal argument passed in", e)
			}
		}
		return false
	}

	companion object {
		private val logger = LoggerFactory.getLogger(ConnectionTester::class.java)
	}
}
