package dev.httpserver.handlers;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.routing.Handler;

public final class HealthHandler implements Handler {

    @Override
    public HttpResponse handle(HttpRequest request) {
        return HttpResponseBuilder.okText("ok");
    }
}
