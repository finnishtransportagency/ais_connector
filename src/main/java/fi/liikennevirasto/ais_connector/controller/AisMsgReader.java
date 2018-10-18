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

import fi.liikennevirasto.ais_connector.client.AisTCPSocketClient;
import fi.liikennevirasto.ais_connector.util.AisConnectionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.PreDestroy;
import java.time.Instant;

public class AisMsgReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisMsgReader.class);

    private final AisTCPSocketClient aisTCPSocketClient;

    private int totalMsgCount = 0;
    private int deltaMsgCount = 0;
    private int intervalCount = 0;
    private Instant printStatsAt = addMinuteTo(Instant.now());

    private Flux<String> aisDataFlux;

    public AisMsgReader(AisTCPSocketClient aisTCPSocketClient) {
        this.aisTCPSocketClient = aisTCPSocketClient;
    }

    public Flux<String> getAisDataFlux() {
        return aisDataFlux;
    }

    public void initAisMsgReading(AisConnectionDetails connDetails) {

        aisTCPSocketClient.connectToAisAndStartReadStream(connDetails);

        aisDataFlux = Flux.create((FluxSink<String> fluxSink) -> {
            while (true) {
                String line = readLine();
                if (line == null) {
                    fluxSink.complete();
                    break;
                }
                fluxSink.next(line);
            }
        });
    }

    private String readLine() {
        String line = aisTCPSocketClient.readLineFromAis();
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
        LOGGER.info("Msg count from last minute: " + deltaCount + ", total from last " + intervals + " minutes: " + totalCount);
    }

    @PreDestroy
    public void destroy() {
        LOGGER.debug("Exiting");
        aisTCPSocketClient.closeAisConnection();
    }
}
