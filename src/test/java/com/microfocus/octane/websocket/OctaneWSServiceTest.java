package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.clients.EndpointClientTestA;
import com.microfocus.octane.websocket.simulator.TestWebSocketsSimulator;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.Test;

public class OctaneWSServiceTest {

	@Test
	public void testA() throws Exception {
		TestWebSocketsSimulator.startWebsocketServer(TestWSServlet.class);

		OctaneWSClientContext contextA = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:3333/messaging/test")
				.setClient("WS_xe6rvwgx6g3pparpxo0xdzj32")
				.setSecret("@ef373863e712e02G")
				.build();

		//  TODO: set properties

		EndpointClientTestA client = new EndpointClientTestA(contextA);
		OctaneWSClientService.getInstance().initClient(client);

		client.sendString("some ping text");
		//  verify result

		client.sendBinary(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		//  verify result
	}

	public static final class TestWSServlet extends WebSocketServlet {

		@Override
		public void configure(WebSocketServletFactory webSocketServletFactory) {
			webSocketServletFactory.getPolicy().setMaxTextMessageSize(256 * 1024);
			webSocketServletFactory.getPolicy().setMaxBinaryMessageSize(256 * 1024);
			webSocketServletFactory.register(TestWSHandler.class);
		}
	}

	public static final class TestWSHandler extends WebSocketAdapter {
		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			System.out.println("some bin");
		}

		@Override
		public void onWebSocketText(String message) {
			System.out.println("some text");
		}
	}
}
