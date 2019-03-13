package com.microfocus.octane.websocket;

import org.junit.Test;

public class OctaneWSServiceNegativeTest {
	private final OctaneWSClientService octaneWSClientService = OctaneWSClientService.getInstance();

	@Test(expected = IllegalArgumentException.class)
	public void testA1() {
		octaneWSClientService.initClient(null);
	}

	//	BUILD WITH MISSING DATA
	@Test(expected = IllegalStateException.class)
	public void testB1() {
		OctaneWSClientContext.builder()
				.build();
	}

	@Test(expected = IllegalStateException.class)
	public void testB2() {
		OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080")
				.build();
	}

	//  ILLEGAL DATA FOR BUILDER
	@Test(expected = IllegalArgumentException.class)
	public void testC1() {
		OctaneWSClientContext.builder()
				.setEndpointUrl(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC21() {
		OctaneWSClientContext.builder()
				.setEndpointUrl("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC22() {
		OctaneWSClientContext.builder()
				.setEndpointUrl("http://localhost:8080");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC3() {
		OctaneWSClientContext.builder()
				.setClient(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC4() {
		OctaneWSClientContext.builder()
				.setClient("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testC5() {
		OctaneWSClientContext.builder()
				.setSecret(null);
	}

	//  USE AFTER BUILT
	@Test(expected = IllegalStateException.class)
	public void testD1() {
		OctaneWSClientContext.OctaneWSClientContextBuilder builder = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080")
				.setClient("some_client")
				.setSecret("some_secret");
		builder.build();

		builder.build();
	}

	@Test(expected = IllegalStateException.class)
	public void testD2() {
		OctaneWSClientContext.OctaneWSClientContextBuilder builder = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080")
				.setClient("some_client")
				.setSecret("some_secret");
		builder.build();

		builder.setEndpointUrl("ws://localhost:8080");
	}

	@Test(expected = IllegalStateException.class)
	public void testD3() {
		OctaneWSClientContext.OctaneWSClientContextBuilder builder = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080")
				.setClient("some_client")
				.setSecret("some_secret");
		builder.build();

		builder.setClient("new_client");
	}

	@Test(expected = IllegalStateException.class)
	public void testD4() {
		OctaneWSClientContext.OctaneWSClientContextBuilder builder = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080")
				.setClient("some_client")
				.setSecret("some_secret");
		builder.build();

		builder.setSecret("new_secret");
	}
}
