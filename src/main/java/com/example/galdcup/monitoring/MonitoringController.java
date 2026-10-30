package com.example.galdcup.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.galdcup.monitoring.MonitoringData.*;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {
    private final MonitoringService service;

    @GetMapping
    public ResponseEntity<Dashboard> dashboard(@RequestParam(defaultValue = "30m") String range) {
        return response(service.dashboard(range));
    }
    @GetMapping("/health")
    public ResponseEntity<List<Dependency>> health() { return response(service.snapshot().health()); }
    @GetMapping("/api")
    public ResponseEntity<ApiMetrics> api() { return response(service.snapshot().api()); }
    @GetMapping("/redis")
    public ResponseEntity<RedisMetrics> redis() { return response(service.snapshot().redis()); }
    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> alerts() { return response(service.snapshot().alerts()); }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
