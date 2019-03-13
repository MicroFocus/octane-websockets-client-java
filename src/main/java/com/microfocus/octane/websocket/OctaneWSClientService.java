package com.microfocus.octane.websocket;

import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Octane's WebSocket clients Service
 * - responsible for setting up global parameters
 * - responsible for creating WebSocket clients per requested context
 */

public class OctaneWSClientService {
	private static final Logger logger = LoggerFactory.getLogger(OctaneWSClientService.class);
	private static final Object INSTANCE_CREATION_LOCK = new Object();
	private static OctaneWSClientService INSTANCE;

	private final WebSocketClient webSocketClient;

	public static OctaneWSClientService getInstance() {
		if (INSTANCE == null) {
			synchronized (INSTANCE_CREATION_LOCK) {
				if (INSTANCE == null) {
					INSTANCE = new OctaneWSClientService();
				}
			}
		}
		return INSTANCE;
	}

	private OctaneWSClientService() {
		webSocketClient = new WebSocketClient();
		webSocketClient.setStopAtShutdown(true);

		try {
			webSocketClient.start();
		} catch (Exception e) {
			logger.error("failed to start native WebSocket client", e);
		}
	}

	/**
	 * starts websocket client connected as per specified context
	 *
	 * @param octaneWSEndpointClient client instance
	 */
	public void initClient(OctaneWSEndpointClient octaneWSEndpointClient) {
		if (octaneWSEndpointClient == null) {
			throw new IllegalArgumentException("ws endpoint client MUST NOT be null");
		}

		octaneWSEndpointClient.start(webSocketClient);
		logger.info("successfully started client to " + octaneWSEndpointClient.getContext());
	}
}
