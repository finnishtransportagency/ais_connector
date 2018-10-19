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
package fi.liikennevirasto.ais_connector.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ais")
public class AisConnectorProperties {

    private Connector connector;

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public static class Connector {

        private String username;
        private String password;
        private int webSocketPort;
        private Socket socket;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getWebSocketPort() {
            return webSocketPort;
        }

        public void setWebSocketPort(int webSocketPort) {
            this.webSocketPort = webSocketPort;
        }

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public static class Socket {

            private int timeout;
            private String keepAliveMessage;
            private String keepAliveResponseMessage;
            private long keepAliveTimeout;
            private String logoffMessage;

            public int getTimeout() {
                return timeout;
            }

            public void setTimeout(int timeout) {
                this.timeout = timeout;
            }

            public String getKeepAliveMessage() {
                return keepAliveMessage;
            }

            public void setKeepAliveMessage(String keepAliveMessage) {
                this.keepAliveMessage = keepAliveMessage;
            }

            public String getKeepAliveResponseMessage() {
                return keepAliveResponseMessage;
            }

            public void setKeepAliveResponseMessage(String keepAliveResponseMessage) {
                this.keepAliveResponseMessage = keepAliveResponseMessage;
            }

            public long getKeepAliveTimeout() {
                return keepAliveTimeout;
            }

            public void setKeepAliveTimeout(long keepAliveTimeout) {
                this.keepAliveTimeout = keepAliveTimeout;
            }

            public String getLogoffMessage() {
                return logoffMessage;
            }

            public void setLogoffMessage(String logoffMessage) {
                this.logoffMessage = logoffMessage;
            }

        }

    }

}
