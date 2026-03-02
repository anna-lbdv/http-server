package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.http.HttpMethod;
import dev.httpserver.routing.Handler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MiddlewareChainTest {

    private static HttpRequest emptyRequest() {
        return HttpRequest.builder()
                .method(HttpMethod.GET)
                .path("/test")
                .build();
    }

    // Ordering

    @Test
    void middlewaresAreCalledInRegistrationOrder() {
        List<String> callOrder = new ArrayList<>();

        Middleware first = (req, ctx, next) -> {
            callOrder.add("first");
            return next.next(req, ctx);
        };
        Middleware second = (req, ctx, next) -> {
            callOrder.add("second");
            return next.next(req, ctx);
        };
        Middleware third = (req, ctx, next) -> {
            callOrder.add("third");
            return next.next(req, ctx);
        };

        Handler handler = req -> {
            callOrder.add("handler");
            return HttpResponseBuilder.okText("ok");
        };

        MiddlewareChain chain = new MiddlewareChain(List.of(first, second, third));
        chain.execute(emptyRequest(), new RequestContext(), handler);

        assertEquals(List.of("first", "second", "third", "handler"), callOrder);
    }

    @Test
    void middlewareCanShortCircuitChain() {
        List<String> callOrder = new ArrayList<>();

        Middleware blocking = (req, ctx, next) -> {
            callOrder.add("blocking");
            return HttpResponseBuilder.error(
                    dev.httpserver.http.HttpStatus.UNAUTHORIZED, "Unauthorized");
        };
        Middleware afterBlock = (req, ctx, next) -> {
            callOrder.add("after");
            return next.next(req, ctx);
        };

        MiddlewareChain chain = new MiddlewareChain(List.of(blocking, afterBlock));
        HttpResponse resp = chain.execute(emptyRequest(), new RequestContext(),
                req -> HttpResponseBuilder.okText("ok"));

        assertEquals(List.of("blocking"), callOrder);
        assertEquals(401, resp.getStatus().getCode());
    }

    // CorrelationIdMiddleware

    @Test
    void correlationIdIsGeneratedWhenMissing() {
        CorrelationIdMiddleware mw = new CorrelationIdMiddleware();
        MiddlewareChain chain = new MiddlewareChain(List.of(mw));

        HttpResponse resp = chain.execute(emptyRequest(), new RequestContext(),
                req -> HttpResponseBuilder.okText("ok"));

        String header = resp.getHeaders().get("X-Request-ID");
        assertNotNull(header, "X-Request-ID header must be present");
        assertFalse(header.isBlank());
    }

    @Test
    void correlationIdIsReusedFromRequest() {
        String existingId = "my-trace-id-123";
        HttpRequest req = HttpRequest.builder()
                .method(HttpMethod.GET)
                .path("/test")
                .headers(java.util.Map.of("x-request-id", existingId))
                .build();

        CorrelationIdMiddleware mw = new CorrelationIdMiddleware();
        MiddlewareChain chain = new MiddlewareChain(List.of(mw));

        HttpResponse resp = chain.execute(req, new RequestContext(),
                r -> HttpResponseBuilder.okText("ok"));

        assertEquals(existingId, resp.getHeaders().get("X-Request-ID"));
    }

    @Test
    void correlationIdIsStoredInContext() {
        RequestContext ctx = new RequestContext();
        CorrelationIdMiddleware mw = new CorrelationIdMiddleware();
        MiddlewareChain chain = new MiddlewareChain(List.of(mw));

        chain.execute(emptyRequest(), ctx, req -> HttpResponseBuilder.okText("ok"));

        assertNotNull(ctx.getCorrelationId());
        assertFalse(ctx.getCorrelationId().isBlank());
    }

    // Handler exception handling

    @Test
    void handlerExceptionResultsIn500() {
        MiddlewareChain chain = new MiddlewareChain(List.of());
        HttpResponse resp = chain.execute(emptyRequest(), new RequestContext(),
                req -> {
                    throw new RuntimeException("boom");
                });

        assertEquals(500, resp.getStatus().getCode());
    }
}
