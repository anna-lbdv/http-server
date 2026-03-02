package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;

@FunctionalInterface
public interface NextHandler {
    HttpResponse next(HttpRequest request, RequestContext ctx) throws Exception;
}
