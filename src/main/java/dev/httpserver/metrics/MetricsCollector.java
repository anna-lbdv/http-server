package dev.httpserver.metrics;

import java.util.concurrent.atomic.AtomicLong;

public final class MetricsCollector {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong requests2xx = new AtomicLong();
    private final AtomicLong requests4xx = new AtomicLong();
    private final AtomicLong requests5xx = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();

    public void record(int statusCode, long latencyMs) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        if (statusCode >= 200 && statusCode < 300) {
            requests2xx.incrementAndGet();
        } else if (statusCode >= 400 && statusCode < 500) {
            requests4xx.incrementAndGet();
        } else if (statusCode >= 500) {
            requests5xx.incrementAndGet();
        }
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getRequests2xx() {
        return requests2xx.get();
    }

    public long getRequests4xx() {
        return requests4xx.get();
    }

    public long getRequests5xx() {
        return requests5xx.get();
    }

    public double getAvgLatencyMs() {
        long total = totalRequests.get();
        return total == 0 ? 0.0 : (double) totalLatencyMs.get() / total;
    }
}
