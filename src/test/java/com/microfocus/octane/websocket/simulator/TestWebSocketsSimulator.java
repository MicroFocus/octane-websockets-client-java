package com.microfocus.octane.websocket.simulator;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;

import javax.servlet.http.HttpServlet;

public class TestWebSocketsSimulator {

	public static Server startWebsocketServer(int port, Class<? extends HttpServlet> httpHandler, Class<? extends JettyWebSocketServlet> wsHandler) throws Exception {

		//  init server
		Server server = new Server();
		ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(port);
		server.addConnector(httpConnector);

		//  add WS servlet
		ServletContextHandler wsServletContextHandler = new ServletContextHandler(null, "/messaging", ServletContextHandler.SESSIONS);
		wsServletContextHandler.addServlet(wsHandler, "/test");

		//  add HTTP servlet
		ServletContextHandler httpServletContextHandler = new ServletContextHandler(null, "/");
		httpServletContextHandler.addServlet(httpHandler, "/");

		server.setHandler(new HandlerList(wsServletContextHandler, httpServletContextHandler));
		server.start();

		return server;
	}
}
