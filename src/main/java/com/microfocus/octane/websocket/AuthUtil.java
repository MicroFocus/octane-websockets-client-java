package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.exceptions.OctaneWSAuthException;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.charset.StandardCharsets;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;

/**
 * Auth util is a stateless helper to perform authentication to Octane based on the provided configuration
 */
class AuthUtil {
	private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);
	private static final String AUTH_RESOURCE = "/authentication/sign_in";
	private static final String COOKIES_HEADER_NAME = "SET-COOKIE";
	private static final String AUTH_COOKIE_NAME = "LWSSO_COOKIE_KEY";

	private AuthUtil() {
	}

	static HttpCookie login(OctaneWSClientContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context MUST NOT be NULL");
		}

		URL loginUrl = buildLoginUrl(context.endpointUrl);
		byte[] loginPayloadBytes = buildLoginPayload(context.client, context.secret);
		Proxy proxy = setupProxy(context.proxyUrl, context.proxyUsername, context.proxyPassword);

		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) loginUrl.openConnection(proxy);
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod(HttpMethod.POST.asString());
			connection.setRequestProperty(HttpHeader.CONTENT_TYPE.asString(), APPLICATION_JSON.asString());
			connection.setRequestProperty(HttpHeader.CONTENT_LENGTH.asString(), String.valueOf(loginPayloadBytes.length));
			connection.getOutputStream().write(loginPayloadBytes);
			connection.connect();

			if (connection.getResponseCode() != HttpStatus.OK_200) {
				throw new OctaneWSAuthException("failed to perform login request " + context + " (response status " + connection.getResponseCode() + " while expected for " + HttpStatus.OK_200 + ")");
			}
		} catch (Exception e) {
			throw new OctaneWSAuthException("failed to perform login request to " + context, e);
		}

		String authTokenValue = retrieveAuthToken(connection);
		if (authTokenValue == null) {
			throw new OctaneWSAuthException("failed to extract auth token from login response");
		}

		return new HttpCookie(AUTH_COOKIE_NAME, authTokenValue);
	}

	private static URL buildLoginUrl(URI endpointUrl) {
		try {
			String url = "ws".equals(endpointUrl.getScheme()) ? "http://" : "https://";
			url += endpointUrl.getHost();
			url += endpointUrl.getPort() != 0 ? (":" + endpointUrl.getPort()) : "";
			url += AUTH_RESOURCE;
			return new URL(url);
		} catch (Exception e) {
			throw new OctaneWSAuthException("failed to create authentication URL to login to access " + endpointUrl, e);
		}
	}

	private static byte[] buildLoginPayload(String client, String secret) {
		String jsonPayload = "{\"client_id\":\"" + client + "\",\"client_secret\":\"" + secret + "\"}";
		return jsonPayload.getBytes(StandardCharsets.UTF_8);
	}

	private static Proxy setupProxy(String proxyUrl, String proxyUsername, String proxyPassword) {
		Proxy result = Proxy.NO_PROXY;
		if (proxyUrl != null && !proxyUrl.isEmpty()) {
			try {
				URL url = new URL(proxyUrl);
				result = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort()));

				if (proxyUsername != null && !proxyUsername.isEmpty()) {
					Authenticator authenticator = new Authenticator() {
						public PasswordAuthentication getPasswordAuthentication() {
							return (new PasswordAuthentication(proxyUsername, (proxyPassword != null ? proxyPassword : "").toCharArray()));
						}
					};
					Authenticator.setDefault(authenticator);
				}

				logger.info("login will be performed via " + result.toString());
			} catch (MalformedURLException murle) {
				logger.error("failed to parse proxy URL " + proxyUrl + ", NO_PROXY will be used", murle);
			}
		}
		return result;
	}

	private static String retrieveAuthToken(URLConnection connection) {
		String result = null;
		String cookiesAll = connection.getHeaderField(COOKIES_HEADER_NAME);
		if (cookiesAll != null && !cookiesAll.isEmpty()) {
			for (String cookiePair : cookiesAll.split(";")) {
				if (cookiePair.isEmpty()) continue;
				String[] cookieKeyValue = cookiePair.split("=");
				if (cookieKeyValue.length != 2 ||
						cookieKeyValue[0].isEmpty() || cookieKeyValue[1].isEmpty() ||
						!AUTH_COOKIE_NAME.equals(cookieKeyValue[0])) continue;
				result = cookieKeyValue[1];
				break;
			}
		}
		return result;
	}
}
