package com.lasthopesoftware.bluewater.shared.exceptions

import android.content.Context
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import org.slf4j.LoggerFactory

class UnexpectedExceptionToasterResponse(private val context: Context) : ImmediateResponse<Throwable?, Void?> {

	companion object {
		private val logger = LoggerFactory.getLogger(UnexpectedExceptionToasterResponse::class.java)
	}

	override fun respond(error: Throwable?): Void? {
		logger.error("An unexpected exception occurred", error)
		UnexpectedExceptionToaster.announce(context, error)
		return null
	}
}
