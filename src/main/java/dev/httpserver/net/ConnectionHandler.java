package dev.httpserver.net;

import dev.httpserver.http.*;
import dev.httpserver.metrics.MetricsCollector;
import dev.httpserver.middleware.MiddlewareChain;
import dev.httpserver.middleware.RequestContext;
import dev.httpserver.routing.Handler;
import dev.httpserver.routing.MatchResult;
import dev.httpserver.routing.Router;
import dev.httpserver.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private final Connection connection;
    private final HttpParser parser;
    private final HttpSerializer serializer;
    private final Router router;
    private final MiddlewareChain middlewareChain;
    private final MetricsCollector metrics;
    private final int requestReadTimeoutMs;
    private final int keepAliveTimeoutMs;
    private final Runnable onComplete;

    public ConnectionHandler(
            Connection connection,
            HttpParser parser,
            HttpSerializer serializer,
            Router router,
            MiddlewareChain middlewareChain,
            MetricsCollector metrics,
            int requestReadTimeoutMs,
            int keepAliveTimeoutMs,
            Runnable onComplete) {
        this.connection = connection;
        this.parser = parser;
        this.serializer = serializer;
        this.router = router;
        this.middlewareChain = middlewareChain;
        this.metrics = metrics;
        this.requestReadTimeoutMs = requestReadTimeoutMs;
        this.keepAliveTimeoutMs = keepAliveTimeoutMs;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        try {
            processConnection();
        } finally {
            connection.close();
            onComplete.run();
        }
    }

    private void processConnection() {
        try {
            InputStream in = connection.getSocket().getInputStream();
            OutputStream out = connection.getSocket().getOutputStream();

            connection.getSocket().setSoTimeout(requestReadTimeoutMs);

            while (!connection.isClosed()) {
                long startMs = TimeUtil.currentMillis();
                HttpResponse response;
                boolean keepAlive = false;

                try {
                    HttpRequest raw = parser.parse(in);

                    String connHeader = raw.getHeader("connection");
                    keepAlive = !"close".equalsIgnoreCase(connHeader);

                    response = dispatch(raw);

                } catch (HttpParser.ParseException ex) {
                    if (ex.getSuggestedStatus() == null) {
                        return; // silent EOF
                    }
                    response = HttpResponseBuilder.error(ex.getSuggestedStatus(), ex.getMessage());
                    keepAlive = false;

                } catch (SocketTimeoutException ex) {
                    log.debug("Socket timeout on {}", connection.getSocket().getRemoteSocketAddress());
                    return;

                } catch (IOException ex) {
                    log.debug("I/O error reading request: {}", ex.getMessage());
                    return;
                }

                response = addHeader(response, "Connection", keepAlive ? "keep-alive" : "close");

                long latency = TimeUtil.currentMillis() - startMs;
                metrics.record(response.getStatus().getCode(), latency);

                try {
                    serializer.serialize(response, out);
                } catch (IOException ex) {
                    log.debug("I/O error writing response: {}", ex.getMessage());
                    return;
                }

                if (!keepAlive)
                    return;

                connection.getSocket().setSoTimeout(keepAliveTimeoutMs);
            }

        } catch (IOException ex) {
            log.debug("Connection setup error: {}", ex.getMessage());
        }
    }

    private HttpResponse dispatch(HttpRequest raw) {
        MatchResult match = router.match(raw.getMethod(), raw.getPath());

        if (match.isNotFound()) {
            return HttpResponseBuilder.error(HttpStatus.NOT_FOUND,
                    "No route for " + raw.getPath());
        }
        if (match.isMethodNotAllowed()) {
            return HttpResponseBuilder.error(HttpStatus.METHOD_NOT_ALLOWED,
                    "Method " + raw.getMethod() + " not allowed for " + raw.getPath());
        }

        HttpRequest enriched = raw.withPathParams(match.getPathParams());
        Handler handler = match.getHandler();
        RequestContext ctx = new RequestContext();

        return middlewareChain.execute(enriched, ctx, handler);
    }

    private static HttpResponse addHeader(HttpResponse response, String name, String value) {
        Map<String, String> headers = new LinkedHashMap<>(response.getHeaders());
        headers.put(name, value);
        return new HttpResponse(response.getStatus(), headers, response.getBody());
    }
}
