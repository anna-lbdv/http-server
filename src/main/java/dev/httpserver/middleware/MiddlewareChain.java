package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.http.HttpStatus;
import dev.httpserver.routing.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class MiddlewareChain {

    private static final Logger log = LoggerFactory.getLogger(MiddlewareChain.class);

    private final List<Middleware> middlewares;

    public MiddlewareChain(List<Middleware> middlewares) {
        this.middlewares = List.copyOf(middlewares);
    }

    public HttpResponse execute(HttpRequest request, RequestContext ctx, Handler finalHandler) {
        Execution exec = new Execution(middlewares, finalHandler);
        try {
            return exec.next(request, ctx);
        } catch (Exception e) {
            log.error("Unhandled exception in middleware/handler pipeline", e);
            return HttpResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private static final class Execution implements NextHandler {
        private final List<Middleware> middlewares;
        private final Handler finalHandler;
        private int index = 0;

        Execution(List<Middleware> middlewares, Handler finalHandler) {
            this.middlewares = middlewares;
            this.finalHandler = finalHandler;
        }

        @Override
        public HttpResponse next(HttpRequest request, RequestContext ctx) throws Exception {
            if (index < middlewares.size()) {
                Middleware mw = middlewares.get(index++);
                return mw.handle(request, ctx, this);
            }
            return finalHandler.handle(request);
        }
    }
}
