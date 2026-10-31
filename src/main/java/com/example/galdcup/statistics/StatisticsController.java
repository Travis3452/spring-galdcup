package com.example.galdcup.statistics;

import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<StatisticsData.Dashboard> dashboard(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.dashboard(days));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> unavailable() {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("message", "서비스 통계를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> invalidRange() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("message", "조회 기간은 1, 7, 30, 90일 중 선택해 주세요."));
    }
}
