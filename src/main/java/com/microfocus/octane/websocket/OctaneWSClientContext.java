package com.microfocus.octane.websocket;

import org.eclipse.jetty.http.HttpScheme;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class OctaneWSClientContext {
	public final URI endpointUrl;
	public final String client;
	public final String secret;
	public final String proxyUrl;
	public final String proxyUsername;
	public final String proxyPassword;
	public final Map<String, String> customHeaders;

	private OctaneWSClientContext(
			URI endpointUrl,
			String client,
			String secret,
			String proxyUrl,
			String proxyUsername,
			String proxyPassword,
			Map<String, String> customHeaders
	) {
		this.endpointUrl = endpointUrl;
		this.client = client;
		this.secret = secret;
		this.proxyUrl = proxyUrl;
		this.proxyUsername = proxyUsername;
		this.proxyPassword = proxyPassword;
		this.customHeaders = Collections.unmodifiableMap(customHeaders == null ? new HashMap<>() : customHeaders);
	}

	public static OctaneWSClientContextBuilder builder() {
		return new OctaneWSClientContextBuilder();
	}

	@Override
	public String toString() {
		return "OctaneWSClientContext { " +
				"endpointUrl: " + endpointUrl +
				", client: " + client +
				", secret: " + (secret == null ? null : (secret.length() < 3 ? "..." : (secret.charAt(0) + "..." + secret.charAt(secret.length() - 1)))) + " }";
	}

	public static final class OctaneWSClientContextBuilder {
		private boolean built = false;
		private URI endpointUrl;
		private String client;
		private String secret;
		private String proxyUrl;
		private String proxyUsername;
		private String proxyPassword;
		private Map<String, String> customHeaders;

		private OctaneWSClientContextBuilder() {
		}

		public OctaneWSClientContextBuilder setEndpointUrl(String endpointUrl) {
			validateBuildState();

			if (endpointUrl == null || endpointUrl.isEmpty()) {
				throw new IllegalArgumentException("endpoint URI MUST NOT be NULL nor EMPTY");
			}

			URI uri;
			try {
				uri = new URI(endpointUrl);
			} catch (URISyntaxException urise) {
				throw new IllegalArgumentException("failed to parse endpoint URI", urise);
			}

			if (!HttpScheme.WS.is(uri.getScheme()) && !HttpScheme.WSS.is(uri.getScheme())) {
				throw new IllegalArgumentException("endpoint URI's schema MUST BE either 'ws' or 'wss'; found " + uri.getScheme());
			}
			if (!uri.isAbsolute()) {
				throw new IllegalArgumentException("endpoint URI MUST BE absolute");
			}

			this.endpointUrl = uri;
			return this;
		}

		public OctaneWSClientContextBuilder setClient(String client) {
			validateBuildState();

			if (client == null || client.isEmpty()) {
				throw new IllegalArgumentException("client MUST NOT be NULL nor EMPTY");
			}

			this.client = client;
			return this;
		}

		public OctaneWSClientContextBuilder setSecret(String secret) {
			validateBuildState();

			if (secret == null) {
				throw new IllegalArgumentException("secret, if/when set, MUST NOT be NULL");
			}

			this.secret = secret;
			return this;
		}

		public OctaneWSClientContextBuilder setProxyUrl(String proxyUrl) {
			validateBuildState();

			if (proxyUrl == null) {
				throw new IllegalArgumentException("proxyUrl, if/when set, MUST NOT be NULL");
			}

			this.proxyUrl = proxyUrl;
			return this;
		}

		public OctaneWSClientContextBuilder setProxyUsername(String proxyUsername) {
			validateBuildState();

			if (proxyUsername == null) {
				throw new IllegalArgumentException("proxy username, if/when set, MUST NOT be NULL");
			}

			this.proxyUsername = proxyUsername;
			return this;
		}

		public OctaneWSClientContextBuilder setProxyPassword(String proxyPassword) {
			validateBuildState();

			if (proxyPassword == null) {
				throw new IllegalArgumentException("proxy password, if/when set, MUST NOT be NULL");
			}

			this.proxyPassword = proxyPassword;
			return this;
		}

		public OctaneWSClientContextBuilder setCustomHeaders(Map<String, String> customHeaders) {
			validateBuildState();

			if (customHeaders == null) {
				throw new IllegalArgumentException("custom headers, if set, MUST NOT be NULL");
			}

			this.customHeaders = customHeaders;
			return this;
		}

		public OctaneWSClientContext build() {
			validateBuildState();
			if (endpointUrl == null) {
				throw new IllegalStateException("endpoint URI MUST NOT be NULL");
			}
			if (client == null || client.isEmpty()) {
				throw new IllegalStateException("client MUST NOT be NULL nor EMPTY");
			}

			if (secret == null) {
				secret = "";
			}

			OctaneWSClientContext result = new OctaneWSClientContext(
					endpointUrl,
					client,
					secret,
					proxyUrl,
					proxyUsername,
					proxyPassword,
					customHeaders
			);
			built = true;
			return result;
		}

		private void validateBuildState() {
			if (built) {
				throw new IllegalStateException("builder, once built, MAY NOT be used any further; please create a new builder");
			}
		}
	}
}
