package com.microfocus.octane.websocket.exceptions;

public class OctaneWSException extends RuntimeException {

	public OctaneWSException(String message) {
		super(message);
	}

	public OctaneWSException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
