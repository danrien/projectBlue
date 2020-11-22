package com.namehillsoftware.projectblue.shared.exceptions;

import java.io.IOException;


public class HttpResponseException extends IOException {
	private final int responseCode;

	public HttpResponseException(int responseCode) {
		super();
		this.responseCode = responseCode;
	}

	public HttpResponseException(int responseCode, String message) {
		super(message);
		this.responseCode = responseCode;
	}

	public HttpResponseException(int responseCode, String message, Throwable cause) {
		super(message, cause);
		this.responseCode = responseCode;
	}

	public HttpResponseException(int responseCode, Throwable cause) {
		super(cause);
		this.responseCode = responseCode;
	}

	public int getResponseCode() {
		return responseCode;
	}
}
