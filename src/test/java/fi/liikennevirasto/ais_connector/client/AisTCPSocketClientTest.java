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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class AisTCPSocketClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisTCPSocketClientTest.class);

    private static final int AIS_DATA_LINES_TO_READ = 5;
    private static final int EXPECTED_MINIMUM_LENGTH_OF_AIS_DATA_LINE = 47;

    private static final String TEST_INPUT = "rtr_aisproxy rtr_aisproxy" +
            "!BSVDM,1,1,,B,1DWi12000H1nRfDR>BR9vFrT08;@,0*03\r\n" +
            "!BSVDM,1,1,,B,1CL?VDgP00R49ULSIV@>4?vT24s0,0*00\r\n" +
            "!ABVDM,1,1,4,B,3D7FKV0vAEQmgTfR:odJrHbT0000,0*0F\r\n" +
            "!BSVDM,1,1,,B,1D4bmT001E1Ft<:RV1QG0mNT08;B,0*42\r\n" +
            "!BSVDM,1,1,,B,1CKG9APP001aKlpTQmDpmgv`0<0J,0*0B\r\n" +
            "!ABVDM,1,1,0,B,3ConKD002AQiAWBUA5=sUqBR01c1,0*77\r\n";

    @Test
    public void readLinesFromAisTCPSocket() throws IOException {

        final Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(TEST_INPUT.getBytes()));

        AisTCPSocketClient aisClient = new AisTCPSocketClientImpl() {
            @Override
            protected Socket createSocket(AisConnectionDetails connDetails) {
                return socket;
            }
        };

        aisClient.connectToAisAndStartReadStream(new AisConnectionDetails(mock(Environment.class)));

        for (int i = 1; i <= AIS_DATA_LINES_TO_READ; i++) {
            String line = aisClient.readLineFromAis();
            LOGGER.info("Line " + i + " value is: " + line);
            assertTrue("Line too short!", line.length() >= EXPECTED_MINIMUM_LENGTH_OF_AIS_DATA_LINE);
        }

        aisClient.closeAisConnection();
    }
}
