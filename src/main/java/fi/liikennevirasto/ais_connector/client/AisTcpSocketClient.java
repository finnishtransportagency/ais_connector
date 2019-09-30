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

import fi.liikennevirasto.ais_connector.configuration.AisConnectorProperties;
import fi.liikennevirasto.ais_connector.util.AisConnectionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AisTcpSocketClient implements AutoCloseable {

    private static final Marker FATAL = MarkerFactory.getMarker("FATAL");
    private static final Logger LOGGER = LoggerFactory.getLogger(AisTcpSocketClient.class);

    private static final Object LOCK = new Object();

    private final AisConnectionDetails connDetails;
    private final AisConnectorProperties aisConnectorProperties;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    private Socket socket;
    private BufferedReader aisReader;
    private Instant lastKeepAliveReply;
    private boolean firstLine = true;

    @Autowired
    public AisTcpSocketClient(AisConnectionDetails connDetails, AisConnectorProperties aisConnectorProperties) {
        this.connDetails = connDetails;
        this.aisConnectorProperties = aisConnectorProperties;
    }

    public boolean connect() throws IOException {
        if (isConnecting.compareAndSet(false, true)) {
            if (!connDetails.isValid()) {
                LOGGER.error(FATAL, "Invalid connection details ({})", connDetails);
                return false;
            }

            LOGGER.info("Connecting to {}", connDetails);

            try {
                socket = createSocket();
                aisReader = createAisReader();
                login();
                isConnected.set(true);
                lastKeepAliveReply = null;
            } finally {
                isConnecting.set(false);
            }

            LOGGER.info("Connected to {}", connDetails);
        }

        return isConnected();
    }

    protected Socket createSocket() throws IOException {
        Socket socket = new Socket(connDetails.getAddress(), connDetails.getPort());

        socket.setSoTimeout(aisConnectorProperties.getConnector().getSocket().getTimeout());
        socket.setKeepAlive(true);

        return socket;
    }

    private void login() throws IOException {
        send(String.format("%c%s%c%s%c", 1, connDetails.getUser(), 0, connDetails.getPasswd(), 0));
    }

    protected BufferedReader createAisReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String readLine() {
        try {
            String line = aisReader.readLine();
            // quick retry #1
            if (line == null) {
                line = aisReader.readLine();
            }

            if (firstLine) { // to skip proxy related text
                line = trimFirstLine(line);
                firstLine = false;
            }

            if (aisConnectorProperties.getConnector().getSocket().getKeepAliveResponseMessage().equals(line)) {
                lastKeepAliveReply = Instant.now();
                line = aisReader.readLine();
            }

            if (line == null) {
                isConnected.set(false); // trigger reconnect
                LOGGER.error(FATAL, "Unable to read from socket. Possible invalid credentials?");
            }

            LOGGER.debug("lineFromAisServer: {}", line);
            return line;
        } catch (IOException e) {
            isConnected.set(false); // trigger reconnect
            LOGGER.error(FATAL, "Unable to read from socket.");
        }
        return null;
    }

    private String trimFirstLine(String line) {
        if (line == null) {
            return null;
        }
        int startIndex = line.indexOf("!");
        if (startIndex > 0) {
            LOGGER.info("Skipped: {}", line.substring(0, startIndex - 1));
            line = line.substring(startIndex);
        }
        return line;
    }

    public boolean isConnected() {
        return socket != null
                && !socket.isClosed()
                && socket.isConnected()
                && isConnected.get()
                && (
                        lastKeepAliveReply == null
                        || lastKeepAliveReply.isAfter(Instant.now().minusMillis(aisConnectorProperties.getConnector().getSocket().getKeepAliveTimeout()))
                );
    }

    public void sendKeepAlive() throws IOException {
        LOGGER.info("Sending Keep-Alive message");
        send(aisConnectorProperties.getConnector().getSocket().getKeepAliveMessage());
    }

    public boolean reconnect() throws Exception {
        close();
        return connect();
    }

    @Override
    public void close() {
        LOGGER.info("Disconnecting from {}", connDetails);
        try {
            logoff();
            socket.close();
        } catch (IOException ex) {
            LOGGER.error("Failed to disconnect");
        }
        LOGGER.info("Disconnected from {}", connDetails);
    }

    private void logoff() throws IOException {
        LOGGER.info("Logging off from {}", connDetails);
        send(aisConnectorProperties.getConnector().getSocket().getLogoffMessage());
        LOGGER.info("Logged off from {}", connDetails);
    }

    private void send(String s) throws IOException {
        synchronized (LOCK) {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeBytes(s);
            dos.flush();
        }
    }

}
