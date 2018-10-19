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
package fi.liikennevirasto.ais_connector.client;

import fi.liikennevirasto.ais_connector.util.AisConnectionDetails;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "ais.scheduling.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AisTcpSocketClientTest {

    private static final int MOCK_SERVER_PORT = SocketUtils.findAvailableTcpPort();

    @Autowired
    private AisTcpSocketClient aisTcpSocketClient;

    private ServerSocket mockServer;

    @Before
    public void before() throws IOException {
        mockServer = new ServerSocket(MOCK_SERVER_PORT);
    }

    @After
    public void after() throws IOException {
        mockServer.close();
    }

    @Test
    public void testReadLinesFromAisTcpSocket() throws Exception {
        List<String> testInput = Arrays.asList(
                "!BSVDM,1,1,,B,1DWi12000H1nRfDR>BR9vFrT08;@,0*03",
                "!BSVDM,1,1,,B,1CL?VDgP00R49ULSIV@>4?vT24s0,0*00",
                "!ABVDM,1,1,4,B,3D7FKV0vAEQmgTfR:odJrHbT0000,0*0F",
                "!BSVDM,1,1,,B,1D4bmT001E1Ft<:RV1QG0mNT08;B,0*42",
                "!BSVDM,1,1,,B,1CKG9APP001aKlpTQmDpmgv`0<0J,0*0B",
                "!ABVDM,1,1,0,B,3ConKD002AQiAWBUA5=sUqBR01c1,0*77"
        );

        Socket socket = mock(Socket.class);
        when(socket.isConnected()).thenReturn(true);
        when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(String.join("\r\n", testInput).getBytes()));

        try (AisTcpSocketClient aisTcpSocketClientSpy = spy(aisTcpSocketClient)) {
            when(aisTcpSocketClientSpy.createSocket()).thenReturn(socket);

            assertTrue(aisTcpSocketClientSpy.connect());

            for (String line : testInput) {
                assertEquals(line, aisTcpSocketClientSpy.readLine());
            }
        }
    }

    @TestConfiguration
    static class AisTcpSocketClientTestConfiguration {

        @Bean
        public AisConnectionDetails aisConnectionDetails() {
            return new AisConnectionDetails(new MockEnvironment()
                    .withProperty("user", "user")
                    .withProperty("passwd", "passwd")
                    .withProperty("address", "localhost")
                    .withProperty("port", String.valueOf(MOCK_SERVER_PORT)));
        }

    }

}
