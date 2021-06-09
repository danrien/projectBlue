package com.lasthopesoftware.bluewater.client.connection.libraries

data class ConnectionSettings(
	val accessCode: String? = null,
	val userName: String? = null,
	val password: String? = null,
	val isLocalOnly: Boolean = false,
	val isWakeOnLanEnabled: Boolean = false)
