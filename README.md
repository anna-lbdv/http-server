# http-server

Simple HTTP/1.1 server written in Java without any frameworks. Made this to practice working with sockets and
multithreading.

## How to run

You need Java 17+ and Maven.

```bash
mvn package -DskipTests
java -jar target/http-server.jar
```

By default starts on port 8080. You can change it:

```bash
java -jar target/http-server.jar --port=9090
```

## Endpoints

```bash
# health check
curl http://localhost:8080/health

# echo with path param and query
curl "http://localhost:8080/echo/42?foo=bar"

# post json body, get it back + serverTime
curl -X POST http://localhost:8080/json \
  -H "Content-Type: application/json" \
  -d '{"name":"anna"}'

# basic metrics
curl http://localhost:8080/metrics
```

## Config

Can be set via CLI or `server.properties`:

| key                    | default   | description                    |
|------------------------|-----------|--------------------------------|
| `port`                 | `8080`    | port to listen on              |
| `host`                 | `0.0.0.0` | bind address                   |
| `workerThreads`        | `16`      | thread pool size               |
| `queueCapacity`        | `256`     | max queued requests before 503 |
| `maxConnections`       | `200`     | max open sockets               |
| `requestReadTimeoutMs` | `5000`    | timeout to read request        |
| `keepAliveTimeoutMs`   | `30000`   | keep-alive idle timeout        |
| `maxHeaderSizeBytes`   | `8192`    | header size limit              |
| `maxBodySizeBytes`     | `1048576` | body size limit (1 MB)         |

## Tests

```bash
mvn test
```

## How it works (briefly)

- One acceptor thread accepts TCP connections
- Connections go into a bounded queue (backpressure — returns 503 if full)
- Fixed thread pool processes requests
- Router matches static paths first, then paths with `{params}`
- Two middlewares: logging and correlation ID (`X-Request-ID`)
- Keep-alive is on by default for HTTP/1.1

## Stack

- Java 17, no frameworks
- Jackson for JSON
- SLF4J + Logback for logging
- JUnit 5 for tests
