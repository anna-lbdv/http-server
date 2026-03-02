package dev.httpserver.routing;

import java.util.Collections;
import java.util.Map;

public final class MatchResult {

    public enum Status {
        // A handler was found for the given method and path
        MATCHED,
        // No route exists for this path at all
        NOT_FOUND,
        // The path is registered but not for the requested HTTP method
        METHOD_NOT_ALLOWED
    }

    private final Status status;
    private final Handler handler;
    private final Map<String, String> pathParams;

    private MatchResult(Status status, Handler handler, Map<String, String> pathParams) {
        this.status = status;
        this.handler = handler;
        this.pathParams = pathParams;
    }

    public static MatchResult matched(Handler handler, Map<String, String> pathParams) {
        return new MatchResult(Status.MATCHED, handler, Collections.unmodifiableMap(pathParams));
    }

    public static MatchResult notFound() {
        return new MatchResult(Status.NOT_FOUND, null, Collections.emptyMap());
    }

    public static MatchResult methodNotAllowed() {
        return new MatchResult(Status.METHOD_NOT_ALLOWED, null, Collections.emptyMap());
    }

    public Status getStatus() {
        return status;
    }

    public Handler getHandler() {
        return handler;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public boolean isMatched() {
        return status == Status.MATCHED;
    }

    public boolean isNotFound() {
        return status == Status.NOT_FOUND;
    }

    public boolean isMethodNotAllowed() {
        return status == Status.METHOD_NOT_ALLOWED;
    }
}
