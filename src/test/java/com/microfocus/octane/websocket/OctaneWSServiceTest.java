package com.microfocus.octane.websocket;

import com.microfocus.octane.websocket.clients.EndpointClientTestA;
import org.junit.Ignore;
import org.junit.Test;

public class OctaneWSServiceTest {

	@Test
	public void testA() {
		OctaneWSClientContext contextA = OctaneWSClientContext.builder()
				.setEndpointUrl("ws://localhost:8080/messaging/shared_spaces/1001/webhooks")
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
}
