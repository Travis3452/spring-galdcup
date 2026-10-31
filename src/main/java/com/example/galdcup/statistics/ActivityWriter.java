package com.example.galdcup.statistics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ActivityWriter {
    private final StatisticsRepository repository;
    public ActivityWriter(StatisticsRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean record(long userId, LocalDate day) {
        if (repository.hasActivity(userId, day)) return false;
        return repository.insertActivity(userId, day);
    }
}
