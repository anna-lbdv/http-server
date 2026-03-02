package dev.httpserver.integration;

import dev.httpserver.bootstrap.HttpServer;
import dev.httpserver.bootstrap.ServerConfig;
import org.junit.jupiter.api.*;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerIntegrationTest {

    private static HttpServer server;
    private static int port;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws Exception {
        // Pick a random free port
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        server = new HttpServer(ServerConfig.load(new String[] { "--port=" + port }));
        server.start();

        // Small delay to ensure acceptor is listening
        Thread.sleep(200);

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
        base = "http://localhost:" + port;
    }

    @AfterAll
    static void stopServer() {
        if (server != null)
            server.stop();
    }

    // /health

    @Test
    @Order(1)
    void healthReturns200() throws Exception {
        HttpResponse<String> resp = get("/health");
        assertEquals(200, resp.statusCode());
        assertEquals("ok", resp.body().trim());
    }

    // /echo/{id}

    @Test
    @Order(2)
    void echoReturnsIdAndQueryParams() throws Exception {
        HttpResponse<String> resp = get("/echo/123?foo=bar&x=1");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"id\":\"123\""), "body must contain id=123: " + body);
        assertTrue(body.contains("\"foo\":\"bar\""), "body must contain foo=bar: " + body);
    }

    // /json

    @Test
    @Order(3)
    void jsonEndpointRoundtrips() throws Exception {
        String payload = "{\"name\":\"anna\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/json"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"name\":\"anna\""), "body must echo name: " + body);
        assertTrue(body.contains("serverTime"), "body must have serverTime: " + body);
    }

    // 404

    @Test
    @Order(4)
    void unknownRouteReturns404() throws Exception {
        HttpResponse<String> resp = get("/does-not-exist");
        assertEquals(404, resp.statusCode());
    }

    // 405

    @Test
    @Order(5)
    void wrongMethodReturns405() throws Exception {
        // /health only supports GET
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/health"))
                .DELETE()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, resp.statusCode());
    }

    // /metrics

    @Test
    @Order(6)
    void metricsContainsCounters() throws Exception {
        HttpResponse<String> resp = get("/metrics");
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("totalRequests"), "metrics must contain totalRequests: " + body);
        assertTrue(body.contains("avgLatencyMs"), "metrics must contain avgLatencyMs: " + body);
    }

    // Keep-alive

    @Test
    @Order(7)
    void multipleRequestsOnSingleConnection() throws Exception {
        // HttpClient with HTTP/1.1 reuses connections by default
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> resp = get("/health");
            assertEquals(200, resp.statusCode(), "Request " + i + " must succeed");
        }
    }

    // X-Request-ID

    @Test
    @Order(8)
    void correlationIdIsReturnedInResponse() throws Exception {
        HttpResponse<String> resp = get("/health");
        String correlationId = resp.headers().firstValue("X-Request-ID").orElse(null);
        assertNotNull(correlationId, "X-Request-ID response header must be present");
        assertFalse(correlationId.isBlank());
    }

    @Test
    @Order(9)
    void incomingCorrelationIdIsEchoed() throws Exception {
        String myId = "test-trace-42";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/health"))
                .header("X-Request-ID", myId)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        String returned = resp.headers().firstValue("X-Request-ID").orElse(null);
        assertEquals(myId, returned);
    }

    // Oversized bod

    @Test
    @Order(10)
    void oversizedBodyReturns413() throws Exception {
        // Default maxBodySizeBytes = 1MB
        byte[] big = new byte[2 * 1024 * 1024];
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/json"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(big))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(413, resp.statusCode());
    }

    // Helpers

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
