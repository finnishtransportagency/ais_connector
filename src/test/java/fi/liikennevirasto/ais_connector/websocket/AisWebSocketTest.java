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
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "ais.scheduling.enabled=false",
        "ais.connector.username=distributor",
        "ais.connector.password=test"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AisWebSocketTest {

    @Autowired
    private AisConnectorProperties aisConnectorProperties;

    @Autowired
    private AisWebSocketServer aisWebSocketServer;

    @Test
    public void testWebSocketRead() throws Exception {
        String expectedAisData = "!ABVDM,1,1,,A,1P000Oh1IT1svTP2r:43grwb05q4,0*01";
        AtomicReference<String> actualAisData = new AtomicReference<>();

        WebSocketClient webSocketClient = new WebSocketClient(createUrl("distributor", "test")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {}
            @Override
            public void onMessage(String message) {
                actualAisData.set(message);
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {}
            @Override
            public void onError(Exception ex) {}
        };

        webSocketClient.connectBlocking(10, TimeUnit.SECONDS);

        aisWebSocketServer.broadcast(expectedAisData);

        for (int i = 0; i < 100 && actualAisData.get() == null; i++) {
            Thread.sleep(100);
        }

        assertEquals(expectedAisData, actualAisData.get());
    }

    @Test
    public void testWebSocketAuthenticationFails() throws Exception {
        AtomicReference<Boolean> actualConnectionClosed = new AtomicReference<>(false);

        WebSocketClient webSocketClient = new WebSocketClient(createUrl("distributor", "invalid")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {}
            @Override
            public void onMessage(String message) {}
            @Override
            public void onClose(int code, String reason, boolean remote) {
                actualConnectionClosed.set(true);
            }
            @Override
            public void onError(Exception ex) {}
        };

        webSocketClient.connectBlocking(10, TimeUnit.SECONDS);

        assertTrue(actualConnectionClosed.get());
    }

    private URI createUrl(String username, String password) throws URISyntaxException {
        return new URI("ws://localhost:" + aisConnectorProperties.getConnector().getWebSocketPort() + "?username=" + username + "&passwd=" + password);
    }

}
