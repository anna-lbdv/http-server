package dev.httpserver.bootstrap;

import dev.httpserver.handlers.*;
import dev.httpserver.http.HttpMethod;
import dev.httpserver.http.HttpParser;
import dev.httpserver.http.HttpSerializer;
import dev.httpserver.metrics.MetricsCollector;
import dev.httpserver.middleware.*;
import dev.httpserver.net.Acceptor;
import dev.httpserver.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class HttpServer {

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    private final ServerConfig config;
    private Acceptor acceptor;
    private ThreadPoolExecutor executor;

    public HttpServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        MetricsCollector metrics = new MetricsCollector();
        HttpParser parser = new HttpParser(config.maxHeaderSizeBytes, config.maxBodySizeBytes);
        HttpSerializer serializer = new HttpSerializer();

        Router router = Router.builder()
                .register(HttpMethod.GET, "/health", new HealthHandler())
                .register(HttpMethod.GET, "/echo/{id}", new EchoHandler())
                .register(HttpMethod.POST, "/json", new JsonHandler())
                .register(HttpMethod.GET, "/metrics", new MetricsHandler(metrics))
                .build();

        MiddlewareChain chain = new MiddlewareChain(List.of(
                new CorrelationIdMiddleware(),
                new LoggingMiddleware()));

        executor = new ThreadPoolExecutor(
                config.workerThreads,
                config.workerThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queueCapacity),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("request-worker-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()); // throw RejectedExecutionException on overflow

        InetAddress bindAddress = InetAddress.getByName(config.host);
        ServerSocket serverSocket = new ServerSocket(config.port, 128, bindAddress);
        serverSocket.setReuseAddress(true);

        acceptor = new Acceptor(serverSocket, executor, config, parser, serializer,
                router, chain, metrics);
        acceptor.start();

        log.info("HTTP server started. {}", config);
    }

    public void stop() {
        log.info("Shutting down HTTP server...");
        if (acceptor != null)
            acceptor.shutdown();
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("HTTP server stopped.");
    }
}