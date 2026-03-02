package dev.httpserver.http;

import java.util.Arrays;

public enum HttpMethod {
    GET, POST, PUT, DELETE;

    public static HttpMethod fromString(String method) {
        if (method == null) return null;
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(method.trim()))
                .findFirst()
                .orElse(null);
    }
}
