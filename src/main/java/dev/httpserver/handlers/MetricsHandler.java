package dev.httpserver.handlers;

import dev.httpserver.http.HttpRequest;
import dev.httpserver.http.HttpResponse;
import dev.httpserver.http.HttpResponseBuilder;
import dev.httpserver.metrics.MetricsCollector;
import dev.httpserver.routing.Handler;

public final class MetricsHandler implements Handler {

    private final MetricsCollector metrics;

    public MetricsHandler(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String body = String.format(
                "totalRequests %d%n" +
                        "requests_2xx  %d%n" +
                        "requests_4xx  %d%n" +
                        "requests_5xx  %d%n" +
                        "avgLatencyMs  %.2f%n",
                metrics.getTotalRequests(),
                metrics.getRequests2xx(),
                metrics.getRequests4xx(),
                metrics.getRequests5xx(),
                metrics.getAvgLatencyMs());

        return HttpResponseBuilder.okText(body);
    }
}
