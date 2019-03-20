package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.exceptions.OctaneWSException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * WebSocket client API, dedicated to interop with Octane's websocket endpoint
 * - each client is oriented to serve only one specific Octane instance
 * - each client is oriented to serve only one specific Octane endpoint
 * - each client will attempt to preserve/renew connection if/when disconnected
 */
public abstract class OctaneWSEndpointClient implements WebSocketListener {
	private static final Logger logger = LoggerFactory.getLogger(OctaneWSEndpointClient.class);
	private final ExecutorService keepAliveService = Executors.newSingleThreadExecutor(new WSEndpointClientKeepAliveThreadFactory());
	private final OctaneWSClientContext context;
	private HttpCookie cachedAuthToken;
	private Session session;

	protected OctaneWSEndpointClient(OctaneWSClientContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context MUST NOT be null");
		}
		this.context = context;
	}

	/**
	 * extensibility point for the consumer implementations to react on binary messages
	 *
	 * @param message binary message
	 */
	public void onBinaryMessage(byte[] message) {
		throw new IllegalStateException("not implemented");
	}

	/**
	 * extensibility point for the consumer implementation to react on string messages
	 *
	 * @param message string message
	 */
	public void onStringMessage(String message) {
		throw new IllegalStateException("not implemented");
	}

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
		session = null;
		logger.info("session to " + context + " has been closed; code: " + code + ", reason: " + reason);
	}

	@Override
	public void onWebSocketConnect(Session session) {
		if (this.session != null && this.session.isOpen()) {
			logger.warn("found opened session while processing onWebSocketConnect event, abnormal behavior");
			this.session.close();
		}
		this.session = session;
		logger.info("session to " + context + " has been opened");
	}

	@Override
	public void onWebSocketError(Throwable throwable) {
		logger.error("session to " + context + " experienced error", throwable);
	}

	public void stop() {
		if (session != null && session.isOpen()) {
			session.close(StatusCode.NORMAL, "client requested to close");
		}
		if (!keepAliveService.isShutdown()) {
			keepAliveService.shutdown();
		}
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

	final void start() {
		boolean done = false;
		int maxAttempts = 2;
		int attempts = 0;
		HttpCookie authToken = cachedAuthToken;

		if (authToken == null) {
			authToken = AuthUtil.login(context);
		}

		while (!done && attempts++ < maxAttempts) {
			try {
				ClientUpgradeRequest upgradeRequest = prepareUpgradeRequest(authToken, context.customHeaders);

				Future<Session> connectPromise = OctaneWSClientService.getInstance()
						.getWebSocketClient()
						.connect(this, context.endpointUrl, upgradeRequest);
				session = connectPromise.get();
				//  TODO: validate session?

				logger.info("starting keep alive worker for client of " + context);
				keepAliveService.execute(this::keepAlive);

				done = true;
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

	private ClientUpgradeRequest prepareUpgradeRequest(HttpCookie authToken, Map<String, String> customHeaders) {
		if (authToken == null) {
			throw new IllegalArgumentException("auth token MUST NOT be NULL");
		}

		ClientUpgradeRequest result = new ClientUpgradeRequest();

		//  set auth token
		result.setCookies(Collections.singletonList(authToken));

		//  set headers
		if (customHeaders != null && !customHeaders.isEmpty()) {
			for (Map.Entry<String, String> header : customHeaders.entrySet()) {
				result.setHeader(header.getKey(), header.getValue());
			}
		}

		return result;
	}

	private void keepAlive() {
		ByteBuffer pingBytes = ByteBuffer.wrap(new byte[]{0});
		while (!keepAliveService.isShutdown()) {
			try {
				if (session.isOpen()) {
					session.getRemote().sendPing(pingBytes);
				} else {
					start();
				}
			} catch (Exception e) {
				logger.error("failed to PING endpoint, will attempt to reconnect if relevant");
				safeSleep(3000);
				if (session == null || !session.isOpen())
					try {
						start();
					} catch (Exception e1) {
						//
					}
			} finally {
				safeSleep(1000);
			}
		}
		logger.info("keep alive worker exited");
	}

	private void validateWorkable() {
		if (session == null) {
			throw new IllegalStateException("endpoint session has not yet been initialized");
		}
		if (!session.isOpen()) {
			throw new IllegalStateException("endpoint session is closed");
		}
	}

	private void safeSleep(long millisToSleep) {
		long started = System.currentTimeMillis();
		while (System.currentTimeMillis() - started < millisToSleep) {
			try {
				Thread.sleep(millisToSleep);
			} catch (InterruptedException ie) {
				logger.warn("interrupted while safe-sleeping", ie);
			}
		}
	}

	private static final class WSEndpointClientKeepAliveThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			Thread result = new Thread(r);
			result.setDaemon(true);
			result.setName("WS endpoint client life keeper: " + result.getId());
			return result;
		}
	}
}
