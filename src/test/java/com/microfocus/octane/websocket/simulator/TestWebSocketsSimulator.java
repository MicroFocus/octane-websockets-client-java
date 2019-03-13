package com.microfocus.octane.websocket.simulator;

import com.microfocus.octane.websocket.clients.BaseHttpServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class TestWebSocketsSimulator {

	public static void startWebsocketServer(Class handler) throws Exception {

		//  init server
		Server server = new Server();
		ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(3333);
		server.addConnector(httpConnector);

		//  add WS servlet
		ServletContextHandler wsServletContextHandler = new ServletContextHandler(null, "/messaging", ServletContextHandler.SESSIONS);
		wsServletContextHandler.addServlet(handler, "/test");

		//  add HTTP servlet
		ServletContextHandler httpServletContextHandler = new ServletContextHandler(null, "/");
		httpServletContextHandler.addServlet(BaseHttpServlet.class, "/");

		server.setHandler(new HandlerList(wsServletContextHandler, httpServletContextHandler));
		server.start();
	}
}
