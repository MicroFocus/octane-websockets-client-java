package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.clients.EndpointClientTestA;
import com.microfocus.octane.websocket.exceptions.OctaneWSAuthException;
import com.microfocus.octane.websocket.simulator.TestWebSocketsSimulator;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class OctaneWSServiceE2ETest {
	private static final Logger logger = LoggerFactory.getLogger(OctaneWSServiceE2ETest.class);
	private static int E2E_SERVER_PORT = 3333;
	private static Server testServer;

	@BeforeClass
	public static void startTestServer() throws Exception {
		logger.info("-- starting test server");
		String portParam = System.getProperty("octane.websockets.client.test.port");
		testServer = TestWebSocketsSimulator.startWebsocketServer(
				portParam != null && !portParam.isEmpty() ? E2E_SERVER_PORT = Integer.parseInt(portParam) : E2E_SERVER_PORT,
				E2ETestHttpServlet.class,
				E2ETestWSServlet.class);
	}

	@AfterClass
	public static void stopTestServer() throws Exception {
		logger.info("-- stopping test server");
		testServer.stop();
		WSTestsUtils.waitAtMostFor(5000, () -> {
			if (testServer.isStopped()) {
				return true;
			} else {
				return null;
			}
		});
	}

	@Test(expected = OctaneWSAuthException.class)
	public void testLoginScenarios() {
		E2ETestHttpServlet.expectedClient = "login_client";
		E2ETestHttpServlet.expectedSecret = "login_secret";
		Map<String, String> headers = new HashMap<>();
		headers.put("header-name", "header-value");

		OctaneWSClientContext context = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:" + E2E_SERVER_PORT + "/messaging/test?param-a=a&param-b=b")
				.setClient("login_client")
				.setSecret("wrong_secret")
				.setCustomHeaders(headers)
				.build();

		EndpointClientTestA client = new EndpointClientTestA(context);
		OctaneWSClientService.getInstance().initClient(client);
	}

	@Test
	public void testInterop() {
		E2ETestHttpServlet.expectedClient = "login_client";
		E2ETestHttpServlet.expectedSecret = "login_secret";
		Map<String, String> headers = new HashMap<>();
		headers.put("header-name", "header-value");

		OctaneWSClientContext contextA = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:" + E2E_SERVER_PORT + "/messaging/test?param-a=a&param-b=b")
				.setClient("login_client")
				.setSecret("login_secret")
				.setCustomHeaders(headers)
				.build();

		EndpointClientTestA client = new EndpointClientTestA(contextA);
		OctaneWSClientService.getInstance().initClient(client);

		//  binary messaging 'ping pong'
		E2ETestWSHandler.lastReceivedBinary = null;
		client.lastReceivedBinary = null;
		client.sendBinary(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
		WSTestsUtils.waitAtMostFor(3000, () -> E2ETestWSHandler.lastReceivedBinary);
		Assert.assertTrue(E2ETestWSHandler.lastReceivedBinary.length == 10 && E2ETestWSHandler.lastReceivedBinary[0] == 0 && E2ETestWSHandler.lastReceivedBinary[5] == 5);
		WSTestsUtils.waitAtMostFor(3000, () -> client.lastReceivedBinary);
		Assert.assertTrue(client.lastReceivedBinary.length == 10 && client.lastReceivedBinary[0] == 0 && client.lastReceivedBinary[5] == 5);

		//  string messaging 'ping pong'
		E2ETestWSHandler.lastReceivedString = null;
		client.lastReceivedString = null;
		client.sendString("some ping text");
		WSTestsUtils.waitAtMostFor(3000, () -> E2ETestWSHandler.lastReceivedString);
		Assert.assertEquals("some ping text", E2ETestWSHandler.lastReceivedString);
		WSTestsUtils.waitAtMostFor(3000, () -> client.lastReceivedString);
		Assert.assertEquals("some ping text", client.lastReceivedString);

		client.stop();
		client.lastReceivedString = null;

		WSTestsUtils.delay(4000);

		Assert.assertNull(client.lastReceivedString);
	}

	@Test
	public void serverDisconnectionTest() throws Exception {
		E2ETestHttpServlet.expectedClient = "login_client";
		E2ETestHttpServlet.expectedSecret = "login_secret";
		Map<String, String> headers = new HashMap<>();
		headers.put("header-name", "header-value");

		OctaneWSClientContext contextA = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:" + E2E_SERVER_PORT + "/messaging/test?param-a=a&param-b=b")
				.setClient("login_client")
				.setSecret("login_secret")
				.setCustomHeaders(headers)
				.build();

		EndpointClientTestA client = new EndpointClientTestA(contextA);
		OctaneWSClientService.getInstance().initClient(client);

		client.lastReceivedString = null;
		client.sendString("some");
		WSTestsUtils.waitAtMostFor(3000, () -> client.lastReceivedString);
		Assert.assertEquals("some", client.lastReceivedString);

		stopTestServer();
		WSTestsUtils.delay(4000);

		startTestServer();
		WSTestsUtils.delay(9000);

		client.lastReceivedString = null;
		client.sendString("else");
		WSTestsUtils.waitAtMostFor(3000, () -> client.lastReceivedString);
		Assert.assertEquals("else", client.lastReceivedString);

		client.stop();
	}

	public static final class E2ETestHttpServlet extends HttpServlet {
		private static String expectedClient;
		private static String expectedSecret;

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			Request request = (Request) req;

			if ("POST".equalsIgnoreCase(req.getMethod()) && "/authentication/sign_in".equals(request.getHttpURI().getPath())) {
				String contentLine = request.getReader().readLine();
				if (contentLine.contains(expectedClient) && contentLine.contains(expectedSecret)) {
					resp.setStatus(HttpStatus.OK_200);
					resp.setHeader("Set-Cookie", "NON_RELEVANT_COOKIE=non_relevant_data;LWSSO_COOKIE_KEY=some_fake_token");
				} else {
					resp.setHeader("Set-Cookie", "NON_RELEVANT_COOKIE=non_relevant_data");
					resp.setStatus(HttpStatus.UNAUTHORIZED_401);
				}
			} else {
				resp.setStatus(HttpStatus.NOT_FOUND_404);
			}
			resp.flushBuffer();

			request.setHandled(true);
		}
	}


	public static final class E2ETestWSServlet extends WebSocketServlet {

		@Override
		public void configure(WebSocketServletFactory webSocketServletFactory) {
			webSocketServletFactory.getPolicy().setMaxTextMessageSize(256 * 1024);
			webSocketServletFactory.getPolicy().setMaxBinaryMessageSize(256 * 1024);
			webSocketServletFactory.register(E2ETestWSHandler.class);
		}
	}

	public static final class E2ETestWSHandler extends WebSocketAdapter {
		private static byte[] lastReceivedBinary;
		private static String lastReceivedString;

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			lastReceivedBinary = payload;
			try {
				this.getSession().getRemote().sendBytes(ByteBuffer.wrap(payload));
			} catch (IOException ioe) {
				//
			}
		}

		@Override
		public void onWebSocketText(String message) {
			lastReceivedString = message;
			try {
				this.getSession().getRemote().sendString(message);
			} catch (IOException ioe) {
				//
			}
		}
	}
}
