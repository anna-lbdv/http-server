package dev.httpserver.http;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpResponseBuilder {

    private HttpStatus status = HttpStatus.OK;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    HttpResponseBuilder() {
    }

    public HttpResponseBuilder status(HttpStatus status) {
        this.status = status;
        return this;
    }

    public HttpResponseBuilder header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public HttpResponseBuilder contentType(String contentType) {
        return header("Content-Type", contentType);
    }

    public HttpResponseBuilder body(byte[] body) {
        this.body = body != null ? body : new byte[0];
        return this;
    }

    public HttpResponseBuilder body(String text) {
        this.body = text.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public HttpResponse build() {
        Map<String, String> finalHeaders = new LinkedHashMap<>(headers);
        finalHeaders.putIfAbsent("Content-Length", String.valueOf(body.length));
        return new HttpResponse(status, finalHeaders, body);
    }

    public static HttpResponse ok(String body, String contentType) {
        return new HttpResponseBuilder()
                .status(HttpStatus.OK)
                .contentType(contentType)
                .body(body)
                .build();
    }

    public static HttpResponse okJson(String json) {
        return ok(json, "application/json; charset=utf-8");
    }

    public static HttpResponse okText(String text) {
        return ok(text, "text/plain; charset=utf-8");
    }

    public static HttpResponse error(HttpStatus status, String message) {
        return new HttpResponseBuilder()
                .status(status)
                .contentType("text/plain; charset=utf-8")
                .body(message)
                .build();
    }
}
