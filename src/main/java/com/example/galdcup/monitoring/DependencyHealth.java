package com.example.galdcup.monitoring;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.example.galdcup.monitoring.MonitoringData.Dependency;

@Component
public class DependencyHealth {
    private final HealthEndpoint health;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2), task -> {
                Thread thread = new Thread(task, "monitoring-health");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public DependencyHealth(HealthEndpoint health) { this.health = health; }

    public List<Dependency> check() {
        Future<String> db = submit("db");
        Future<String> redis = submit("redis");
        return List.of(new Dependency("api", "Spring Boot API", "UP"),
                new Dependency("db", "Oracle DB", status(db)),
                new Dependency("redis", "Redis 연결", status(redis)));
    }

    private Future<String> submit(String component) {
        try {
            return executor.submit(() -> {
                var result = health.healthForPath(component);
                return result == null ? "UNKNOWN" : result.getStatus().getCode();
            });
        } catch (java.util.concurrent.RejectedExecutionException e) { return null; }
    }

    private String status(Future<String> future) {
        if (future == null) return "UNKNOWN";
        try { return future.get(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return "UNKNOWN"; }
        catch (Exception e) { return "UNKNOWN"; }
        finally { if (!future.isDone()) future.cancel(true); }
    }

    @PreDestroy
    public void close() { executor.shutdownNow(); }
}
