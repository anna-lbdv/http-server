package dev.httpserver.net;

import dev.httpserver.bootstrap.ServerConfig;
import dev.httpserver.http.HttpParser;
import dev.httpserver.http.HttpSerializer;
import dev.httpserver.http.HttpStatus;
import dev.httpserver.metrics.MetricsCollector;
import dev.httpserver.middleware.MiddlewareChain;
import dev.httpserver.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Acceptor extends Thread {

    private static final Logger log = LoggerFactory.getLogger(Acceptor.class);

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final ServerConfig config;
    private final HttpParser parser;
    private final HttpSerializer serializer;
    private final Router router;
    private final MiddlewareChain middlewareChain;
    private final MetricsCollector metrics;
    private final Semaphore connectionSemaphore;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public Acceptor(
            ServerSocket serverSocket,
            ExecutorService executor,
            ServerConfig config,
            HttpParser parser,
            HttpSerializer serializer,
            Router router,
            MiddlewareChain middlewareChain,
            MetricsCollector metrics) {
        super("http-acceptor");
        setDaemon(false);
        this.serverSocket = serverSocket;
        this.executor = executor;
        this.config = config;
        this.parser = parser;
        this.serializer = serializer;
        this.router = router;
        this.middlewareChain = middlewareChain;
        this.metrics = metrics;
        this.connectionSemaphore = new Semaphore(config.maxConnections);
    }

    @Override
    public void run() {
        log.info("Acceptor listening on {}:{}", config.host, config.port);
        while (running.get()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException ex) {
                if (running.get()) {
                    log.error("Accept error: {}", ex.getMessage());
                }
                break; // serverSocket closed
            }

            if (!connectionSemaphore.tryAcquire()) {
                log.warn("maxConnections={} reached, rejecting {}", config.maxConnections,
                        socket.getRemoteSocketAddress());
                reject503(socket);
                continue;
            }

            Connection conn = new Connection(socket);
            Runnable release = connectionSemaphore::release;

            ConnectionHandler handler = new ConnectionHandler(
                    conn, parser, serializer, router, middlewareChain, metrics,
                    config.requestReadTimeoutMs, config.keepAliveTimeoutMs, release);

            try {
                executor.execute(handler);
            } catch (RejectedExecutionException ex) {
                log.warn("Worker queue full, rejecting {}", socket.getRemoteSocketAddress());
                release.run();
                reject503(socket);
            }
        }
        log.info("Acceptor stopped");
    }

    public void shutdown() {
        running.set(false);
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private static void reject503(Socket socket) {
        try (OutputStream out = socket.getOutputStream()) {
            String body = HttpStatus.SERVICE_UNAVAILABLE.getReason();
            String response = "HTTP/1.1 503 Service Unavailable\r\n"
                    + "Content-Type: text/plain; charset=utf-8\r\n"
                    + "Content-Length: " + body.length() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n"
                    + body;
            out.write(response.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
