package com.microfocus.octane.websocket.exceptions;

public class OctaneWSAuthException extends RuntimeException {

	public OctaneWSAuthException(String message) {
		super(message);
	}

	public OctaneWSAuthException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
