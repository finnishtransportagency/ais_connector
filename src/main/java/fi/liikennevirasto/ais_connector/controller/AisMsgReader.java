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
package fi.liikennevirasto.ais_connector.controller;

import fi.liikennevirasto.ais_connector.client.AisTcpSocketClient;
import fi.liikennevirasto.ais_connector.websocket.AisWebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;

@Component
public class AisMsgReader implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisMsgReader.class);

    private final AisTcpSocketClient aisTcpSocketClient;
    private final AisWebSocketServer aisWebSocketServer;

    private int totalMsgCount = 0;
    private int deltaMsgCount = 0;
    private int intervalCount = 0;
    private Instant printStatsAt = addMinuteTo(Instant.now());

    public AisMsgReader(AisTcpSocketClient aisTcpSocketClient, AisWebSocketServer aisWebSocketServer) {
        this.aisTcpSocketClient = aisTcpSocketClient;
        this.aisWebSocketServer = aisWebSocketServer;
    }

    @PostConstruct
    public void setUp() {
        Executors.newSingleThreadExecutor().submit(this);
    }

    @Override
    public void run() {
        try {
            if (aisTcpSocketClient.connect()) {
                while (true) {
                    String aisMsg = readLine();
                    if (aisMsg != null) {
                        aisWebSocketServer.broadcast(aisMsg);
                    }
                }
            } else {
                throw new AisMsgReaderException("Unable to establish connection to VTS server");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to establish connection", e);
        }
    }

    private String readLine() {
        String line = aisTcpSocketClient.readLine();
        updateStats();
        return line;
    }

    private void updateStats() {
        totalMsgCount++;
        deltaMsgCount++;

        if (Instant.now().isAfter(printStatsAt)) {
            printStats(deltaMsgCount, ++intervalCount, totalMsgCount);
            printStatsAt = addMinuteTo(printStatsAt);
            deltaMsgCount = 0;
        }
    }

    private Instant addMinuteTo(Instant instant) {
        return instant.plusSeconds(60);
    }

    private void printStats(int deltaCount, int intervals, int totalCount) {
        LOGGER.info("Msg count from last minute: {}, total from last {} minutes: {}", deltaCount, intervals, totalCount);
    }

    @PreDestroy
    public void destroy() {
        LOGGER.debug("Exiting");
        try {
            aisTcpSocketClient.close();
        } catch (Exception e) {
            LOGGER.error("Failed to close connection", e);
        }
    }
}
