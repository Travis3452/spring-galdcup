package com.example.galdcup.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.galdcup.monitoring.MonitoringData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringServiceTests {
    private PrometheusClient prometheus;
    private DependencyHealth dependencies;
    private MonitoringService service;
    private Map<String, Double> values;

    @BeforeEach
    void setUp() throws Exception {
        prometheus = mock(PrometheusClient.class);
        dependencies = mock(DependencyHealth.class);
        values = new HashMap<>(Map.of("springUp", 1.0, "springAge", 10.0, "exporterUp", 1.0,
                "redisUp", 1.0, "redisAge", 10.0, "rps", 0.0, "memory", 1658664.0));
        when(prometheus.current()).thenReturn(values);
        when(prometheus.history(anyString())).thenReturn(new History(true, Map.of()));
        when(dependencies.check()).thenReturn(List.of(new Dependency("api", "API", "UP"),
                new Dependency("db", "Oracle DB", "UP"), new Dependency("redis", "Redis", "UP")));
        service = new MonitoringService(prometheus, dependencies);
    }

    @Test
    void cachesSnapshotsAndHistoryAcrossAdminRequests() throws Exception {
        var first = service.dashboard("30m");
        var second = service.dashboard("30m");
        assertSame(first.snapshot(), second.snapshot());
        assertSame(first.history(), second.history());
        service.dashboard("1h");
        verify(prometheus, times(1)).current();
        verify(dependencies, times(1)).check();
        verify(prometheus, times(1)).history("30m");
        verify(prometheus, times(1)).history("1h");
        assertEquals(0.0, first.snapshot().api().requestsPerSecond());
        assertNull(first.snapshot().api().averageMs());
        assertEquals("UP", first.snapshot().redis().status());
        assertTrue(first.snapshot().alerts().isEmpty());
    }

    @Test
    void unavailablePrometheusIsNeverReportedAsHealthyOrZero() throws Exception {
        when(prometheus.current()).thenThrow(new IOException("private upstream response"));
        when(prometheus.history(anyString())).thenThrow(new IOException("private upstream response"));
        var result = service.dashboard("30m");
        assertFalse(result.snapshot().prometheusAvailable());
        assertNull(result.snapshot().api().requestsPerSecond());
        assertNull(result.snapshot().redis().memoryBytes());
        assertEquals("UNKNOWN", result.snapshot().redis().status());
        assertFalse(result.history().available());
        assertTrue(result.snapshot().alerts().stream().anyMatch(a -> a.id().equals("prometheus")));
        assertFalse(result.toString().contains("private upstream response"));
    }

    @Test
    void staleSamplesAndDownTargetsHideOldMeasurements() {
        values.put("springAge", 91.0);
        values.put("redisUp", 0.0);
        values.put("rps", 300.0);
        var snapshot = service.snapshot();
        assertNull(snapshot.api().requestsPerSecond());
        assertNull(snapshot.redis().memoryBytes());
        assertEquals("DOWN", snapshot.redis().status());
        assertTrue(snapshot.alerts().stream().anyMatch(a -> a.id().equals("spring-scrape")));
        assertFalse(snapshot.alerts().stream().anyMatch(a -> a.id().equals("traffic")));
    }

    @Test
    void missingRedisSamplesAreUnknownInsteadOfDown() {
        values.remove("redisUp");
        assertEquals("UNKNOWN", service.snapshot().redis().status());
    }

    @Test
    void detectsDependencyFailuresServerErrorsTrafficAndRateLimits() {
        when(dependencies.check()).thenReturn(List.of(new Dependency("db", "Oracle DB", "DOWN")));
        values.put("serverErrors", 5.0);
        values.put("rps", 50.0);
        values.put("rateLimited", 0.1);
        var ids = service.snapshot().alerts().stream().map(Alert::id).toList();
        assertTrue(ids.containsAll(List.of("db", "5xx", "traffic", "rate-limit")));
    }

    @Test
    void rejectsArbitraryRangesBeforeUpstreamCalls() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.dashboard("365d"));
        verifyNoInteractions(prometheus, dependencies);
    }
}
