package dev.httpserver.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.http.HttpStatus;
import dev.httpserver.routing.Handler;

import java.time.Instant;

public final class JsonHandler implements Handler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public HttpResponse handle(HttpRequest request) throws Exception {
        byte[] rawBody = request.getBody();
        if (rawBody == null || rawBody.length == 0) {
            return HttpResponseBuilder.error(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        ObjectNode node;
        try {
            node = MAPPER.readValue(rawBody, ObjectNode.class);
        } catch (Exception e) {
            return HttpResponseBuilder.error(HttpStatus.BAD_REQUEST, "Invalid JSON: " + e.getMessage());
        }

        node.put("serverTime", Instant.now().toString());
        return HttpResponseBuilder.okJson(MAPPER.writeValueAsString(node));
    }
}
