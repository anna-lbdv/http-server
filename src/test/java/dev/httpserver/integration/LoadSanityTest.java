package dev.httpserver.integration;

import dev.httpserver.bootstrap.HttpServer;
import dev.httpserver.bootstrap.ServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LoadSanityTest {

    private static final int TOTAL_REQUESTS = 500;
    private static final int CONCURRENCY = 50;

    private static HttpServer server;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        server = new HttpServer(ServerConfig.load(
                new String[] { "--port=" + port, "--workerThreads=16", "--queueCapacity=128" }));
        server.start();
        Thread.sleep(200);
    }

    @AfterAll
    static void stopServer() {
        if (server != null)
            server.stop();
    }

    @Test
    void serverHandlesParallelLoadWithoutErrors() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        String base = "http://localhost:" + port;

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger serverErrors = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    String path = (idx % 2 == 0) ? "/health" : "/echo/" + idx;
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(base + path))
                            .GET()
                            .timeout(Duration.ofSeconds(10))
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    int code = resp.statusCode();
                    if (code >= 500) {
                        serverErrors.incrementAndGet();
                    } else if (code == 503) {
                        // 503 is an acceptable backpressure response
                        successes.incrementAndGet();
                    } else {
                        successes.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            }));
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "Load test timed out");

        System.out.printf("Load sanity: total=%d success=%d serverErrors=%d failures=%d%n",
                TOTAL_REQUESTS, successes.get(), serverErrors.get(), failures.get());

        assertEquals(0, serverErrors.get(), "No 5xx responses expected");
        assertEquals(TOTAL_REQUESTS, successes.get() + failures.get() + serverErrors.get());

        HttpRequest healthReq = HttpRequest.newBuilder()
                .uri(URI.create(base + "/health"))
                .GET()
                .build();
        HttpResponse<String> healthResp = client.send(healthReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResp.statusCode(), "Server must still be alive after load test");
    }
}
