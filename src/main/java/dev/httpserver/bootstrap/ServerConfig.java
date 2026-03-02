package dev.httpserver.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ServerConfig {

    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    public final String host;
    public final int port;
    public final int maxConnections;
    public final int requestReadTimeoutMs;
    public final int keepAliveTimeoutMs;
    public final int maxHeaderSizeBytes;
    public final int maxBodySizeBytes;
    public final int workerThreads;
    public final int queueCapacity;

    private ServerConfig(Builder b) {
        this.host = b.host;
        this.port = b.port;
        this.maxConnections = b.maxConnections;
        this.requestReadTimeoutMs = b.requestReadTimeoutMs;
        this.keepAliveTimeoutMs = b.keepAliveTimeoutMs;
        this.maxHeaderSizeBytes = b.maxHeaderSizeBytes;
        this.maxBodySizeBytes = b.maxBodySizeBytes;
        this.workerThreads = b.workerThreads;
        this.queueCapacity = b.queueCapacity;
    }

    @Override
    public String toString() {
        return String.format(
                "ServerConfig{host='%s', port=%d, maxConnections=%d, " +
                        "requestReadTimeoutMs=%d, keepAliveTimeoutMs=%d, " +
                        "maxHeaderSizeBytes=%d, maxBodySizeBytes=%d, " +
                        "workerThreads=%d, queueCapacity=%d}",
                host, port, maxConnections,
                requestReadTimeoutMs, keepAliveTimeoutMs,
                maxHeaderSizeBytes, maxBodySizeBytes,
                workerThreads, queueCapacity);
    }

    public static ServerConfig load(String[] args) {
        Properties defaults = loadClasspathProperties();
        Properties cli = parseCliArgs(args);

        Properties merged = new Properties(defaults);
        merged.putAll(cli);

        Builder b = new Builder();
        b.host = get(merged, "host", "0.0.0.0");
        b.port = getInt(merged, "port", 8080);
        b.maxConnections = getInt(merged, "maxConnections", 200);
        b.requestReadTimeoutMs = getInt(merged, "requestReadTimeoutMs", 5000);
        b.keepAliveTimeoutMs = getInt(merged, "keepAliveTimeoutMs", 30000);
        b.maxHeaderSizeBytes = getInt(merged, "maxHeaderSizeBytes", 8192);
        b.maxBodySizeBytes = getInt(merged, "maxBodySizeBytes", 1_048_576);
        b.workerThreads = getInt(merged, "workerThreads", 16);
        b.queueCapacity = getInt(merged, "queueCapacity", 256);
        return new ServerConfig(b);
    }

    private static Properties loadClasspathProperties() {
        Properties p = new Properties();
        try (InputStream in = ServerConfig.class.getClassLoader()
                .getResourceAsStream("server.properties")) {
            if (in != null)
                p.load(in);
        } catch (IOException e) {
            log.warn("Could not load server.properties from classpath: {}", e.getMessage());
        }
        return p;
    }

    /** Parses {@code --key=value} and {@code key=value} CLI arguments. */
    private static Properties parseCliArgs(String[] args) {
        Properties p = new Properties();
        if (args == null)
            return p;
        for (String arg : args) {
            String stripped = arg.startsWith("--") ? arg.substring(2) : arg;
            int eq = stripped.indexOf('=');
            if (eq > 0) {
                p.setProperty(stripped.substring(0, eq).trim(), stripped.substring(eq + 1).trim());
            }
        }
        return p;
    }

    private static String get(Properties p, String key, String def) {
        return p.getProperty(key, def);
    }

    private static int getInt(Properties p, String key, int def) {
        String v = p.getProperty(key);
        if (v == null)
            return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for '{}': '{}', using default {}", key, v, def);
            return def;
        }
    }

    private static final class Builder {
        String host;
        int port;
        int maxConnections;
        int requestReadTimeoutMs;
        int keepAliveTimeoutMs;
        int maxHeaderSizeBytes;
        int maxBodySizeBytes;
        int workerThreads;
        int queueCapacity;
    }
}
