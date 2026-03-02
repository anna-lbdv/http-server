package dev.httpserver.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpRequest {

    private final HttpMethod method;
    private final String path;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Map<String, String> pathParams;

    private HttpRequest(Builder b) {
        this.method = b.method;
        this.path = b.path;
        this.queryParams = Collections.unmodifiableMap(new HashMap<>(b.queryParams));
        this.headers = Collections.unmodifiableMap(new HashMap<>(b.headers));
        this.body = b.body != null ? b.body : new byte[0];
        this.pathParams = Collections.unmodifiableMap(new HashMap<>(b.pathParams));
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public byte[] getBody() {
        return body;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    /**
     * Returns a new request with the given path parameters merged in.
     * Used by the router to attach matched path variables.
     */
    public HttpRequest withPathParams(Map<String, String> pathParams) {
        return new Builder()
                .method(this.method)
                .path(this.path)
                .queryParams(this.queryParams)
                .headers(this.headers)
                .body(this.body)
                .pathParams(pathParams)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HttpMethod method;
        private String path;
        private Map<String, String> queryParams = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private byte[] body;
        private Map<String, String> pathParams = new HashMap<>();

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryParams(Map<String, String> qp) {
            this.queryParams = qp;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder pathParams(Map<String, String> pp) {
            this.pathParams = pp;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
