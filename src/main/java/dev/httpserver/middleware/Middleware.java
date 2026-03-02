package dev.httpserver.middleware;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;

public interface Middleware {
    HttpResponse handle(HttpRequest request, RequestContext ctx, NextHandler next) throws Exception;
}
