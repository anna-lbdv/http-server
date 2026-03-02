package dev.httpserver.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpResponse {

    private final HttpStatus status;
    private final Map<String, String> headers;
    private final byte[] body;

    public HttpResponse(HttpStatus status, Map<String, String> headers, byte[] body) {
        this.status = status;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body != null ? body : new byte[0];
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }
}
