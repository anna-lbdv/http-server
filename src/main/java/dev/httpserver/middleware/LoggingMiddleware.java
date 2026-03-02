package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingMiddleware implements Middleware {

    private static final Logger log = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public HttpResponse handle(HttpRequest request, RequestContext ctx, NextHandler next) throws Exception {
        ctx.setStartTimeMs(TimeUtil.currentMillis());
        HttpResponse response = next.next(request, ctx);
        long latency = TimeUtil.currentMillis() - ctx.getStartTimeMs();

        String msg = "req-id={} {} {} -> {} ({}ms)";
        int code = response.getStatus().getCode();
        Object[] args = { ctx.getCorrelationId(), request.getMethod(), request.getPath(), code, latency };

        if (code >= 500) {
            log.error(msg, args);
        } else if (code >= 400) {
            log.warn(msg, args);
        } else {
            log.info(msg, args);
        }

        return response;
    }
}
