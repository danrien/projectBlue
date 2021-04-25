package com.lasthopesoftware.bluewater.shared.exceptions;

import android.content.Context;
import com.namehillsoftware.handoff.promises.response.ImmediateResponse;

public class UnexpectedExceptionToasterResponse implements ImmediateResponse<Throwable, Void> {

	private final Context context;

	public UnexpectedExceptionToasterResponse(Context context) {
		this.context = context;
	}

	@Override
	public Void respond(Throwable error) {
		UnexpectedExceptionToaster.announce(context, error);

		return null;
	}
}
