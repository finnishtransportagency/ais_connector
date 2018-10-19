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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AisTcpSocketClientHealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(AisTcpSocketClientHealthCheck.class);

    private final AisTcpSocketClient aisTcpSocketClient;

    @Autowired
    public AisTcpSocketClientHealthCheck(AisTcpSocketClient aisTcpSocketClient) {
        this.aisTcpSocketClient = aisTcpSocketClient;
    }

    @Scheduled(
            initialDelayString = "${ais.connector.socket.keep-alive-initial-delay}",
            fixedDelayString = "${ais.connector.socket.keep-alive-fixed-delay}"
    )
    public void keepAlive() {
        if (aisTcpSocketClient.isConnected()) {
            try {
                aisTcpSocketClient.sendKeepAlive();
            } catch (IOException e) {
                LOGGER.error("Failed to send Keep-Alive", e);
            }
        } else {
            try {
                aisTcpSocketClient.reconnect();
            } catch (Exception e) {
                LOGGER.error("Failed to reconnect", e);
            }
        }
    }

}
