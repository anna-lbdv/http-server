package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.util.IdGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CorrelationIdMiddleware implements Middleware {

    private static final String REQUEST_HEADER = "x-request-id"; // lowercase for comparison
    private static final String RESPONSE_HEADER = "X-Request-ID";

    @Override
    public HttpResponse handle(HttpRequest request, RequestContext ctx, NextHandler next) throws Exception {
        String correlationId = request.getHeader(REQUEST_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = IdGenerator.generate();
        }
        ctx.setCorrelationId(correlationId);

        HttpResponse response = next.next(request, ctx);

        // Return the response with X-Request-ID injected
        return withExtraHeader(response, RESPONSE_HEADER, correlationId);
    }

    private static HttpResponse withExtraHeader(HttpResponse original, String name, String value) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(name, value);
        headers.putAll(original.getHeaders());
        return new HttpResponse(original.getStatus(), headers, original.getBody());
    }
}
