package com.example.galdcup.statistics;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class ActivityService {
    private final ActivityWriter writer;
    private final Clock clock;
    public ActivityService(ActivityWriter writer, Clock statisticsClock) { this.writer = writer; this.clock = statisticsClock; }

    public void record(long userId) {
        try { writer.record(userId, LocalDate.now(clock.withZone(StatisticsRepository.KOREA))); }
        // The writer's own transaction has rolled back before this is caught.
        catch (DuplicateKeyException ignored) { }
    }
}
