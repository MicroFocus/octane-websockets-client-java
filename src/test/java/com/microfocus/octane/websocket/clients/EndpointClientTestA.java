package com.microfocus.octane.websocket.clients;

import com.microfocus.octane.websocket.OctaneWSEndpointClient;
import com.microfocus.octane.websocket.OctaneWSClientContext;

public class EndpointClientTestA extends OctaneWSEndpointClient {

	public EndpointClientTestA(OctaneWSClientContext context) {
		super(context);
	}

	@Override
	public void onStringMessage(String message) {
		System.out.println("something");
	}

	@Override
	public void onBinaryMessage(byte[] message) {
		System.out.println("something else");
	}
}
