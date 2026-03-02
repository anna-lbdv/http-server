package dev.httpserver.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load(args);
        HttpServer server = new HttpServer(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received");
            server.stop();
        }, "shutdown-hook"));

        server.start();
    }
}
