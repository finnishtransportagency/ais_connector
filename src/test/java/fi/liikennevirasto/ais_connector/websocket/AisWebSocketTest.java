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

import fi.liikennevirasto.ais_connector.client.AisTCPSocketClient;
import fi.liikennevirasto.ais_connector.util.AisConnectionDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.netty.http.client.HttpClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "ais.connector.username=distributor",
        "ais.connector.password=ENC(6Z9/QV2QVz19CmV6mSO+Rw==)"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AisWebSocketTest {

    @MockBean
    private AisTCPSocketClient aisTCPSocketClient;

    @LocalServerPort
    private String port;

    static {
        System.setProperty("jasypt.encryptor.password", "password");
    }

    @Test
    public void testWebSocketRead() throws URISyntaxException {
        String expectedAisData = "!ABVDM,1,1,,A,1P000Oh1IT1svTP2r:43grwb05q4,0*01";
        MonoProcessor<String> output = MonoProcessor.create();

        doNothing().when(aisTCPSocketClient).connectToAisAndStartReadStream(any());
        when(aisTCPSocketClient.readLineFromAis()).thenAnswer(new Answer<String>() {
            private int i = 0;

            @Override
            public String answer(InvocationOnMock invocation) {
                return i++ == 0 ? expectedAisData : null; // only 1 message, null completes Flux
            }
        });

        WebSocketClient webSocketClient = new ReactorNettyWebSocketClient();
        webSocketClient.execute(createUrl("/ais-data", "distributor", "test"), session -> session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .subscribeWith(output)
                .then()
        ).block(Duration.ofSeconds(30));

        String actualAisData = output.block(Duration.ofSeconds(30));

        assertEquals(expectedAisData, actualAisData);
    }

    @Test(expected = HttpClientException.class)
    public void testWebSocketAuthenticationFails() throws URISyntaxException {

        WebSocketClient webSocketClient = new ReactorNettyWebSocketClient();
        webSocketClient.execute(createUrl("/ais-data", "distributor", "wrong"), session -> session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .then()
        ).block(Duration.ofSeconds(30));
    }

    private URI createUrl(String path, String username, String password) throws URISyntaxException {
        return new URI("ws://localhost:" + port + path + "?username=" + username + "&passwd=" + password);
    }

    @TestConfiguration
    static class AisWebSocketTestConfiguration {

        @Bean
        @Primary
        public AisConnectionDetails aisConnectionDetails() {
            MockEnvironment env = new MockEnvironment();

            env.setProperty("user", "user");
            env.setProperty("passwd", "passwd");
            env.setProperty("address", "address");
            env.setProperty("port", "8080");

            return new AisConnectionDetails(env);
        }

    }

}
