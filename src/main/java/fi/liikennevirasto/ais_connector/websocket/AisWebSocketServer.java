/*-
 * ====================================START=======================================
 * ais-connector
 * -----
 * Copyright (C) 2018 Digia
 * -----
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * =====================================END========================================
 */
package fi.liikennevirasto.ais_connector.websocket;

import fi.liikennevirasto.ais_connector.configuration.AisConnectorProperties;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

@Component
public class AisWebSocketServer extends WebSocketServer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisWebSocketServer.class);

    private static final String USERNAME = "username";
    private static final String PASSWD = "passwd";

    private final AisConnectorProperties aisConnectorProperties;

    @Autowired
    public AisWebSocketServer(AisConnectorProperties aisConnectorProperties) {
        super(new InetSocketAddress(aisConnectorProperties.getConnector().getWebSocketPort()));
        this.aisConnectorProperties = aisConnectorProperties;
    }

    @PostConstruct
    public void setUp() {
        start();
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);

        if (!credentialsOk(request)) {
            throw new InvalidDataException(CloseFrame.REFUSE, "Unauthorized");
        }

        return builder;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {}

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

    @Override
    public void onMessage(WebSocket conn, String message) {}

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("Error occurred", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("Started WebSocket server on port {}", aisConnectorProperties.getConnector().getWebSocketPort());
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    private boolean credentialsOk(ClientHandshake request) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(request.getResourceDescriptor()).build().getQueryParams();
        return aisConnectorProperties.getConnector().getUsername().equals(params.getFirst(USERNAME))
                && aisConnectorProperties.getConnector().getPassword().equals(params.getFirst(PASSWD));
    }

}
