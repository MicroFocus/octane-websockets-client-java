package com.microfocus.octane.websocket.clients;

import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BaseHttpServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request request = (Request) req;
		if ("/".equals(request.getHttpURI().getPath())) {
			resp.setStatus(200);
		} else if ("/authentication/sign_in".equals(request.getHttpURI().getPath())) {
			resp.setStatus(200);
			resp.setHeader("Set-Cookie", "LWSSO_COOKIE_KEY=some_fake_token");
		} else {
			resp.setStatus(404);
		}
		resp.flushBuffer();
	}
}
