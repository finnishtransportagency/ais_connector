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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class AisWebSocketService extends HandshakeWebSocketService {

    private static final String USERNAME = "username";
    private static final String PASSWD = "passwd";

    @Value("${ais.connector.username}")
    private String username;
    @Value("${ais.connector.password}")
    private String password;

    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
        return credentialsOk(exchange) ?
                super.handleRequest(exchange, handler) :
                Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private boolean credentialsOk(ServerWebExchange exchange) {
        MultiValueMap<String, String> params = exchange.getRequest().getQueryParams();
        return this.username.equals(params.getFirst(USERNAME)) && this.password.equals(params.getFirst(PASSWD));
    }
}
