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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

@Service
public class AisTCPSocketClientImpl implements AisTCPSocketClient {

    private Socket clientSocket;
    private BufferedReader fromAisServer;
    private boolean firstLine = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(AisTCPSocketClientImpl.class);

    @Override
    public void connectToAisAndStartReadStream(AisConnectionDetails connDetails) {
        LOGGER.debug("aisConnectionDetails: " + connDetails.toString());

        if (clientSocket == null) {
            LOGGER.info("Creating new socket connection");
            try {
                clientSocket = createSocket(connDetails);
                fromAisServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                sendCredentials(connDetails, clientSocket);
                LOGGER.info("Socket connection created");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        } else {
            LOGGER.info("Socket connection already exists.");
        }
    }

    protected Socket createSocket(AisConnectionDetails connDetails) throws IOException {
        return new Socket(connDetails.getAddress(), connDetails.getPort());
    }

    private void sendCredentials(AisConnectionDetails connDetails, Socket socket) throws IOException {
        DataOutputStream toAisServer = new DataOutputStream(socket.getOutputStream());
        toAisServer.writeBytes(String.format("%c%s%c%s%c", 1, connDetails.getUser(), 0, connDetails.getPasswd(), 0));
        toAisServer.flush();
    }

    @Override
    public void closeAisConnection() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
                LOGGER.info("Closed AIS TCP Socket");
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
            clientSocket = null;
        } else {
            LOGGER.info("AIS Socket connection didn't exist");
        }
    }

    @Override
    public String readLineFromAis() {
        try {
            String line = fromAisServer.readLine();
            // quick retry #1
            if (line == null) {
                line = fromAisServer.readLine();
            }

            if (firstLine) { // to skip proxy related text
                line = trimFirstLine(line);
                firstLine = false;
            }

            LOGGER.debug("lineFromAisServer: " + line);
            return line;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private String trimFirstLine(String line) {
        if (line == null) {
            return null;
        }
        int startIndex = line.indexOf("!");
        if (startIndex > 0) {
            LOGGER.info("Skipped: " + line.substring(0, startIndex - 1));
            line = line.substring(startIndex);
        }
        return line;
    }
}
