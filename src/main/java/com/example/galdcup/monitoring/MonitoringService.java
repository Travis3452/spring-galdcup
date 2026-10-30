package com.example.galdcup.monitoring;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.galdcup.monitoring.MonitoringData.*;

@Service
public class MonitoringService {
    private final PrometheusClient prometheus;
    private final DependencyHealth dependencies;
    private Snapshot cached;
    private final Map<String, CachedHistory> histories = new HashMap<>();

    public MonitoringService(PrometheusClient prometheus, DependencyHealth dependencies) {
        this.prometheus = prometheus;
        this.dependencies = dependencies;
    }

    public Dashboard dashboard(String range) {
        PrometheusClient.rangeSeconds(range);
        return new Dashboard(snapshot(), range, history(range));
    }

    public synchronized Snapshot snapshot() {
        if (cached != null && cached.observedAt().plusSeconds(15).isAfter(Instant.now())) return cached;
        boolean available = true;
        Map<String, Double> values;
        try { values = prometheus.current(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); values = Map.of(); available = false; }
        catch (Exception e) { values = Map.of(); available = false; }
        List<Dependency> health = dependencies.check();
        boolean springFresh = fresh(values.get("springAge")) && one(values.get("springUp"));
        String redisStatus = !fresh(values.get("redisAge")) || values.get("exporterUp") == null ? "UNKNOWN"
                : !one(values.get("exporterUp")) ? "DOWN"
                : values.get("redisUp") == null ? "UNKNOWN" : one(values.get("redisUp")) ? "UP" : "DOWN";
        ApiMetrics api = new ApiMetrics(metric(values, "rps", springFresh), metric(values, "averageMs", springFresh),
                metric(values, "maxMs", springFresh), metric(values, "clientErrors", springFresh),
                metric(values, "serverErrors", springFresh), metric(values, "rateLimited", springFresh));
        boolean redisFresh = "UP".equals(redisStatus);
        RedisMetrics redis = new RedisMetrics(metric(values, "memory", redisFresh), metric(values, "maxMemory", redisFresh),
                metric(values, "clients", redisFresh), metric(values, "hitPercent", redisFresh), redisStatus);
        List<Alert> alerts = new ArrayList<>();
        if (!available) alerts.add(new Alert("prometheus", "warning", "지표 서버에 연결할 수 없습니다", "현재 수집 상태와 성능 지표를 확인할 수 없습니다."));
        else {
            if (!springFresh) alerts.add(new Alert("spring-scrape", "critical", "API 지표 수집 중단", "Spring Boot 수집 대상 또는 최근 수집 시각을 확인해 주세요."));
            if (!redisFresh) alerts.add(new Alert("redis-scrape", "warning", "Redis 지표 확인 필요", "Exporter 연결과 Redis 수집 상태를 확인해 주세요."));
        }
        for (Dependency component : health) {
            if (!"UP".equals(component.status())) alerts.add(new Alert(component.id(),
                    "DOWN".equals(component.status()) || "OUT_OF_SERVICE".equals(component.status()) ? "critical" : "warning",
                    component.name() + " 상태 확인 필요", "현재 상태: " + component.status() + ". 연결 상태를 확인해 주세요."));
        }
        if (api.serverErrorPercent() != null && api.serverErrorPercent() >= 5)
            alerts.add(new Alert("5xx", "critical", "서버 오류 비율 상승", "최근 5분 HTTP 5xx 비율이 5% 이상입니다."));
        if (api.requestsPerSecond() != null && api.requestsPerSecond() >= 50)
            alerts.add(new Alert("traffic", "warning", "높은 API 요청량", "최근 5분 평균 요청량이 50 RPS 이상입니다. 평소 트래픽과 비교해 주세요."));
        if (api.rateLimitedPerSecond() != null && api.rateLimitedPerSecond() > 0)
            alerts.add(new Alert("rate-limit", "warning", "요청 제한 발생", "최근 5분에 HTTP 429 응답이 발생했습니다."));
        cached = new Snapshot(Instant.now(), available, health, api, redis, List.copyOf(alerts));
        return cached;
    }

    private synchronized History history(String range) {
        CachedHistory previous = histories.get(range);
        if (previous != null && previous.expiresAt().isAfter(Instant.now())) return previous.value();
        History history;
        try { history = prometheus.history(range); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); history = new History(false, Map.of()); }
        catch (Exception e) { history = new History(false, Map.of()); }
        histories.put(range, new CachedHistory(Instant.now().plusSeconds(30), history));
        return history;
    }

    private static boolean fresh(Double age) { return age != null && age >= 0 && age <= 90; }
    private static boolean one(Double value) { return value != null && value == 1; }
    private static Double metric(Map<String, Double> values, String key, boolean usable) {
        Double value = values.get(key);
        return usable && value != null && Double.isFinite(value) ? Math.max(0, value) : null;
    }
    private record CachedHistory(Instant expiresAt, History value) { }
}
