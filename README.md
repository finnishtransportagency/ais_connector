# AIS data gateway connector

Spring Boot application that connects to TCP-socket to read ASI data stream (IEC 61162 specification). 

Publishes a WebSocket for reading the received stream without any conversions.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- Java is installed
- Maven is installed
- Git client is installed

There is a TCP-socket service available for reading the input stream.

### Compiling and running

1. clone the repository
2. go to root folder
3. build the project with Maven (e.g. "mvn clean install")
4. create additional properties file e.g. credentials.yml with syntax:
   ```
   ais:
     connector:
       username: ****
       password: ****
   ```
5. run application with command:
   ```
   java -jar target\ais-connector-[version].jar --spring.profiles.active=local --user=**** --passwd=**** --address=[tcp_socket_address] --port=[port] --spring.config.additional-location=file:[credentials.yml]
   ```

WebSocket is published to port 8100 that is configured in the application.yml.