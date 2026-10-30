package com.example.galdcup.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class MonitoringData {
    private MonitoringData() { }

    public record Point(long timestamp, Double value) { }
    public record History(boolean available, Map<String, List<Point>> series) { }
    public record Dependency(String id, String name, String status) { }
    public record ApiMetrics(Double requestsPerSecond, Double averageMs, Double rollingMaxMs,
                             Double clientErrorPercent, Double serverErrorPercent, Double rateLimitedPerSecond) { }
    public record RedisMetrics(Double memoryBytes, Double maxMemoryBytes, Double connectedClients,
                               Double hitPercent, String status) { }
    public record Alert(String id, String severity, String title, String description) { }
    public record Snapshot(Instant observedAt, boolean prometheusAvailable, List<Dependency> health,
                           ApiMetrics api, RedisMetrics redis, List<Alert> alerts) { }
    public record Dashboard(Snapshot snapshot, String range, History history) { }
}
