package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.exceptions.OctaneWSException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.Future;

/**
 * WebSocket client API, dedicated to interop with Octane's websocket endpoint
 * - each client is oriented to serve only one specific Octane instance
 * - each client is oriented to serve only one specific Octane endpoint
 */
public abstract class OctaneWSEndpointClient implements WebSocketListener {
	private static final Logger logger = LoggerFactory.getLogger(OctaneWSEndpointClient.class);

	private final OctaneWSClientContext context;
	private HttpCookie cachedAuthToken;
	private Session session;

	protected OctaneWSEndpointClient(OctaneWSClientContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context MUST NOT be null");
		}
		if (context.endpointUrl == null) {
			throw new IllegalArgumentException("WS endpoint URI MUST NOT be NULL");
		}
		if (context.client == null || context.client.isEmpty()) {
			throw new IllegalArgumentException("client MUST NOT be NULL nor EMPTY");
		}
		this.context = context;
	}

	/**
	 * extensibility point for the consumer implementations to react on binary messages
	 *
	 * @param message binary message
	 */
	abstract public void onBinaryMessage(byte[] message);

	/**
	 * extensibility point for the consumer implementation to react on string messages
	 *
	 * @param message string message
	 */
	abstract public void onStringMessage(String message);

	@Override
	public void onWebSocketBinary(byte[] message, int offset, int len) {
		onBinaryMessage(message);
	}

	@Override
	public void onWebSocketText(String message) {
		onStringMessage(message);
	}

	@Override
	public void onWebSocketClose(int code, String reason) {
		logger.info("session to " + context + " has been closed; code: " + code + ", reason: " + reason);
	}

	@Override
	public void onWebSocketConnect(Session session) {
		logger.info("session to " + context + " established connection");
	}

	@Override
	public void onWebSocketError(Throwable throwable) {
		logger.error("session to " + context + " experienced error", throwable);
	}

	final public void sendString(String message) {
		validateWorkable();
		try {
			session.getRemote().sendString(message);
		} catch (IOException ioe) {
			logger.error("failed to send string to " + context, ioe);
			throw new OctaneWSException("failed to send string to " + context, ioe);
		}
	}

	final public void sendBinary(byte[] message) {
		validateWorkable();
		try {
			session.getRemote().sendBytes(ByteBuffer.wrap(message));
		} catch (IOException ioe) {
			logger.error("failed to send string to " + context, ioe);
			throw new OctaneWSException("failed to send string to " + context, ioe);
		}
	}

	/**
	 * INTERNALS
	 */

	final OctaneWSClientContext getContext() {
		return context;
	}

	final void start(WebSocketClient socketClient) {
		int maxAttempts = 2;
		int attempts = 0;
		HttpCookie authToken = cachedAuthToken;

		if (authToken == null) {
			authToken = AuthUtil.login(context);
		}

		while (attempts++ < maxAttempts) {
			try {
				ClientUpgradeRequest upgradeRequest = prepareUpgradeRequest(authToken);
				Future<Session> connectPromise = socketClient.connect(this, context.endpointUrl, upgradeRequest);
				session = connectPromise.get();
				//  TODO: validate session?
			} catch (Exception e) {
				if (e.getCause() != null && e.getCause() instanceof UpgradeException && ((UpgradeException) e.getCause()).getResponseStatusCode() == HttpStatus.UNAUTHORIZED_401) {
					logger.warn("failed to connect to " + context + " due to authentication (401); attempt " + attempts + " out of max " + maxAttempts);
					authToken = AuthUtil.login(context);
				} else {
					throw new OctaneWSException("finally failed to connect to " + context, e);
				}
			}
		}

		if (session == null) {
			throw new OctaneWSException("finally failed to connect to " + context + ", see previous logs for more info / errors");
		} else {
			cachedAuthToken = authToken;
		}
	}

	private ClientUpgradeRequest prepareUpgradeRequest(HttpCookie authToken) {
		if (authToken == null) {
			throw new IllegalArgumentException("auth token MUST NOT be NULL");
		}

		ClientUpgradeRequest result = new ClientUpgradeRequest();

		//  set auth token
		result.setCookies(Collections.singletonList(authToken));

		//  set headers

		return result;
	}

	private void validateWorkable() {
		if (session == null) {
			throw new IllegalStateException("endpoint session has not yet been initialized");
		}
		if (!session.isOpen()) {
			throw new IllegalStateException("endpoint session is closed");
		}
	}
}
