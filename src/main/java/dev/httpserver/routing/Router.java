package dev.httpserver.routing;

import dev.httpserver.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Router {

    private final List<Route> staticRoutes;
    private final List<Route> parametricRoutes;

    private Router(List<Route> staticRoutes, List<Route> parametricRoutes) {
        this.staticRoutes = List.copyOf(staticRoutes);
        this.parametricRoutes = List.copyOf(parametricRoutes);
    }

    public MatchResult match(HttpMethod method, String path) {
        boolean pathExistsWithDifferentMethod = false;

        for (Route route : staticRoutes) {
            Map<String, String> params = route.matchPath(path);
            if (params != null) {
                if (route.getMethod() == method) {
                    return MatchResult.matched(route.getHandler(), params);
                }
                pathExistsWithDifferentMethod = true;
            }
        }

        for (Route route : parametricRoutes) {
            Map<String, String> params = route.matchPath(path);
            if (params != null) {
                if (route.getMethod() == method) {
                    return MatchResult.matched(route.getHandler(), params);
                }
                pathExistsWithDifferentMethod = true;
            }
        }

        return pathExistsWithDifferentMethod
                ? MatchResult.methodNotAllowed()
                : MatchResult.notFound();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Route> staticRoutes = new ArrayList<>();
        private final List<Route> parametricRoutes = new ArrayList<>();

        public Builder register(HttpMethod method, String pattern, Handler handler) {
            Route route = new Route(method, pattern, handler);
            if (route.isParametric()) {
                parametricRoutes.add(route);
            } else {
                staticRoutes.add(route);
            }
            return this;
        }

        public Router build() {
            return new Router(staticRoutes, parametricRoutes);
        }
    }
}