package dev.httpserver.routing;

import dev.httpserver.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Route {

    private final HttpMethod method;
    private final String pattern;
    private final Handler handler;
    private final boolean parametric;
    private final Pattern compiledPattern; // null for static routes
    private final List<String> paramNames;

    public Route(HttpMethod method, String pattern, Handler handler) {
        this.method = method;
        this.pattern = pattern;
        this.handler = handler;
        this.paramNames = new ArrayList<>();

        if (pattern.contains("{")) {
            this.parametric = true;
            this.compiledPattern = buildPattern(pattern, paramNames);
        } else {
            this.parametric = false;
            this.compiledPattern = null;
        }
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPattern() {
        return pattern;
    }

    public Handler getHandler() {
        return handler;
    }

    public boolean isParametric() {
        return parametric;
    }


    public Map<String, String> matchPath(String path) {
        if (!parametric) {
            return pattern.equals(path) ? new HashMap<>() : null;
        }
        Matcher m = compiledPattern.matcher(path);
        if (!m.matches())
            return null;

        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < paramNames.size(); i++) {
            params.put(paramNames.get(i), m.group(i + 1));
        }
        return params;
    }

    private static Pattern buildPattern(String pathPattern, List<String> paramNames) {
        StringBuilder regex = new StringBuilder("^");
        // Split on '/' but keep empty leading segment for paths that start with '/'
        String[] segments = pathPattern.split("/", -1);

        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if (i == 0 && seg.isEmpty()) {
                continue;
            }
            regex.append('/');
            if (seg.startsWith("{") && seg.endsWith("}")) {
                String name = seg.substring(1, seg.length() - 1);
                paramNames.add(name);
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(seg));
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
}
