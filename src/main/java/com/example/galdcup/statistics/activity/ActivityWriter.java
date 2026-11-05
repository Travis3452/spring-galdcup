package com.example.galdcup.statistics.activity;
import com.example.galdcup.statistics.domain.StatisticsRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ActivityWriter {
    private final StatisticsRepository repository;
    public ActivityWriter(StatisticsRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordDailyActivityIfAbsent(long userId, LocalDate day) {
        if (repository.hasActivity(userId, day)) return false;
        return repository.insertActivity(userId, day);
    }
}
