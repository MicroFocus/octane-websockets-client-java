package com.microfocus.octane.websocket.clients;

import com.microfocus.octane.websocket.OctaneWSEndpointClient;
import com.microfocus.octane.websocket.OctaneWSClientContext;

public class EndpointClientTestA extends OctaneWSEndpointClient {
	public byte[] lastReceivedBinary;
	public String lastReceivedString;

	public EndpointClientTestA(OctaneWSClientContext context) {
		super(context);
	}

	@Override
	public void onBinaryMessage(byte[] message) {
		lastReceivedBinary = message;
	}

	@Override
	public void onStringMessage(String message) {
		lastReceivedString = message;
	}
}
