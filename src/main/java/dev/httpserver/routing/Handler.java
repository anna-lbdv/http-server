package dev.httpserver.routing;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;

@FunctionalInterface
public interface Handler {
    HttpResponse handle(HttpRequest request) throws Exception;
}
