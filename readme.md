### Summary

`octane-websockets-client` library is dedicated to simplify consumption of Octane's WebSocket APIs by consumers.

The library provides simple configuration means which used afterwards to establish WS connectivity.

Given credentials are provided, the `octane-websockets-client` will perform the authentication management against Octane under the hood.

Proxy configuration is supported as well.

### Basic walk through

Regular usage of library will involve the below steps:
- create `OctaneWSClientContext` starting with `OctaneWSClientContext.builder()` API
- implement your WS endpoint handler extending `OctaneWSEndpointClient`
- create instance of your WS endpoint with the previously created context object
- initialize the client with `OctaneWSClientService.getInstance().initClient(client)`

Assuming, that your WS endpoint implementation class is `MyOctaneWSEndpointHandler`, the whole process will look like this:

```java
//  Octane WS/WSS endpoints will always start from 'messaging'
//  proxy settings are optionals
OctaneWSClientContext context = OctaneWSClientContext.builder()
		.setEndpointUrl("ws://some.octane.host:1111/messaging/some/ws/endpoint?param=value")
		.setClient("your_ws_username")
		.setSecret("your_ws_password")
		.setProxyUrl("http://web-proxy.host:2222")
		.setProxyUsername("proxy_username_if_relevant")
		.setProxyPassword("proxy_password_if_relevant")
		.build();

OctaneWSEndpointClient client = new MyOctaneWSEndpointHandler(context);
OctaneWSClientService.getInstance().initClient(client);

//  the client is ready to send and receive WS messages
``` 