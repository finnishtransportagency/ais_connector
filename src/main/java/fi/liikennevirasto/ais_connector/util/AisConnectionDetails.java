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
package fi.liikennevirasto.ais_connector.util;

import org.springframework.core.env.Environment;

public class AisConnectionDetails {

    private static final String USER_ARG = "user";
    private static final String PASSWD_ARG = "passwd";
    private static final String ADDRESS_ARG = "address";
    private static final String PORT_ARG = "port";

    private final String user;
    private final String passwd;
    private final String address;
    private final Integer port;

    public AisConnectionDetails(Environment env) {
        this.user = env.getProperty(USER_ARG);
        this.passwd = env.getProperty(PASSWD_ARG);
        this.address = env.getProperty(ADDRESS_ARG);
        this.port = toInteger(env.getProperty(PORT_ARG));
    }

    private Integer toInteger(String port) {
        try {
            return Integer.valueOf(port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getUser() {
        return user;
    }

    public String getPasswd() {
        return passwd;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public boolean isValid() {
        return user != null && passwd != null && address != null && port != null;
    }

    @Override
    public String toString() {
        return "AisConnectionDetails{" +
                "user='" + user + '\'' +
                ", passwd='*****'" +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
