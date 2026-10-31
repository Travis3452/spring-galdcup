package com.example.galdcup.statistics;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.galdcup.statistics.StatisticsData.*;

@Service
public class StatisticsService {
    private final StatisticsRepository repository;
    private final Clock clock;
    private final Map<Integer, Dashboard> cache = new HashMap<>();

    public StatisticsService(StatisticsRepository repository, Clock statisticsClock) {
        this.repository = repository;
        this.clock = statisticsClock;
    }

    public synchronized Dashboard dashboard(int days) {
        if (!List.of(1, 7, 30, 90).contains(days)) throw new IllegalArgumentException("조회 기간은 1, 7, 30, 90일 중 선택해 주세요.");
        LocalDate today = LocalDate.now(clock.withZone(StatisticsRepository.KOREA));
        Dashboard previous = cache.get(days);
        if (previous != null && previous.asOfDate().equals(today)
                && previous.generatedAt().plusSeconds(15).isAfter(clock.instant())) return previous;

        LocalDate from = today.minusDays(days - 1L);
        Totals totals = repository.totals();
        var counts = repository.activeUsers(today);
        LocalDate firstActivity = repository.firstActivityDate();
        boolean noActivity = firstActivity == null;
        ActiveUsers active = new ActiveUsers(noActivity ? null : counts.dau(), noActivity ? null : counts.wau(),
                noActivity ? null : counts.mau(), firstActivity, partial(firstActivity, today),
                partial(firstActivity, today.minusDays(6)), partial(firstActivity, today.minusDays(29)));

        Map<LocalDate, Long> byDay = repository.signups(from, today.plusDays(1));
        LocalDate firstSignup = repository.firstSignupDate();
        // A concurrent first signup can appear between these reads; only understate coverage.
        if (firstSignup == null && !byDay.isEmpty()) firstSignup = byDay.keySet().stream().min(LocalDate::compareTo).orElse(null);
        List<SignupPoint> points = new ArrayList<>();
        long total = 0;
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            Long count = byDay.get(day);
            if (count == null && firstSignup != null && !day.isBefore(firstSignup)) count = 0L;
            points.add(new SignupPoint(day, count));
            if (count != null) total += count;
        }
        Signups signups = new Signups(from, today, firstSignup, firstSignup == null ? null : total,
                partial(firstSignup, from), List.copyOf(points));
        Dashboard result = new Dashboard(clock.instant(), today, "Asia/Seoul", totals, active, signups);
        cache.put(days, result);
        return result;
    }

    private boolean partial(LocalDate first, LocalDate from) { return first == null || !first.isBefore(from); }
}
