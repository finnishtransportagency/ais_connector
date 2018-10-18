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
import fi.liikennevirasto.ais_connector.controller.AisMsgReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AisWebSocketConfiguration {

    private final AisConnectorProperties aisConnectorProperties;

    @Autowired
    public AisWebSocketConfiguration(AisConnectorProperties aisConnectorProperties) {
        this.aisConnectorProperties = aisConnectorProperties;
    }

    @Bean
    public Flux<String> aisData(AisMsgReader aisMsgReader) {
        return aisMsgReader.getAisDataFlux().publish().autoConnect();
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(Flux<String> aisData) {
        Map<String, Object> urlMap = new HashMap<>();
        urlMap.put(aisConnectorProperties.getConnector().getAisDataEndpoint(), new AisWebSocketHandler(aisData));

        SimpleUrlHandlerMapping simpleUrlHandlerMapping = new SimpleUrlHandlerMapping();
        simpleUrlHandlerMapping.setOrder(10);
        simpleUrlHandlerMapping.setUrlMap(urlMap);

        return simpleUrlHandlerMapping;
    }

    @Bean
    AisWebSocketService webSocketService() {
        return new AisWebSocketService();
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter(AisWebSocketService webSocketService) {
        return new WebSocketHandlerAdapter(webSocketService);
    }
}
