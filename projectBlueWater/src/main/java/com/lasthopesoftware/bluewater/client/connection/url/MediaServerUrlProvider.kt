package com.lasthopesoftware.bluewater.client.connection.url

import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLEncoder

class MediaServerUrlProvider private constructor(
	override val authCode: String?,
	baseUrl: URL,
	override val certificateFingerprint: ByteArray) : IUrlProvider
{

	private val baseURL = URL(baseUrl, "/MCWS/v1/")

	constructor(authCode: String?, ipAddress: String?, port: Int, certificateFingerprint: ByteArray)
		: this(authCode, URL(com.namehillsoftware.projectblue.shared.IoCommon.httpsUriScheme, ipAddress, port, ""), certificateFingerprint)

	constructor(authCode: String?, ipAddress: String?, port: Int)
		: this(authCode, URL(com.namehillsoftware.projectblue.shared.IoCommon.httpUriScheme, ipAddress, port, ""))

	constructor(authCode: String?, baseUrl: URL)
		: this(authCode, baseUrl, ByteArray(0))

	override val baseUrl: String
		get() = baseURL.toString()

	override fun getUrl(vararg params: String): String? { // Add base url
		if (params.isEmpty()) return baseUrl

		val urlBuilder = StringBuilder(baseUrl)

		// Add action
		urlBuilder.append(params[0])

		if (params.size == 1) return urlBuilder.toString()

		// add arguments
		for (i in 1 until params.size) {
			urlBuilder.append(if (i == 1) '?' else '&')
			val param = params[i]
			val equalityIndex = param.indexOf('=')
			if (equalityIndex < 0) {
				urlBuilder.append(encodeParameter(param))
				continue
			}
			urlBuilder.append(encodeParameter(param.substring(0, equalityIndex))).append('=').append(encodeParameter(param.substring(equalityIndex + 1)))
		}

		return urlBuilder.toString()
	}

	companion object {
		private fun encodeParameter(parameter: String): String {
			return try {
				URLEncoder.encode(parameter, "UTF-8").replace("+", "%20")
			} catch (e: UnsupportedEncodingException) {
				parameter
			}
		}
	}

}
