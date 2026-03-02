package dev.httpserver.middleware;

import java.util.HashMap;
import java.util.Map;

public final class RequestContext {

    private String correlationId;
    private long startTimeMs;
    private final Map<String, Object> attributes = new HashMap<>();

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String id) {
        this.correlationId = id;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(long ms) {
        this.startTimeMs = ms;
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public Object get(String key) {
        return attributes.get(key);
    }
}
