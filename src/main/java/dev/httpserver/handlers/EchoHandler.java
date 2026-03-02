package dev.httpserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.routing.Handler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoHandler implements Handler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public HttpResponse handle(HttpRequest request) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", request.getPathParams().getOrDefault("id", ""));
        body.put("query", request.getQueryParams());

        return HttpResponseBuilder.okJson(MAPPER.writeValueAsString(body));
    }
}
