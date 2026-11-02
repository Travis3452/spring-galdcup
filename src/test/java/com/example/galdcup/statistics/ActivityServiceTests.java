package com.example.galdcup.statistics;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityServiceTests {
    @Test
    void concurrentDuplicateIsSuccessfulButOtherDatabaseFailuresAreNotHidden() {
        ActivityWriter writer = mock(ActivityWriter.class);
        var clock = Clock.fixed(Instant.parse("2026-10-31T15:01:00Z"), ZoneOffset.UTC);
        var day = LocalDate.of(2026, 11, 1);
        ActivityService service = new ActivityService(writer, clock);
        when(writer.recordDailyActivityIfAbsent(7L, day)).thenThrow(new DuplicateKeyException("duplicate"));
        assertDoesNotThrow(() -> service.record(7L));
        doThrow(new DataAccessResourceFailureException("offline")).when(writer).recordDailyActivityIfAbsent(7L, day);
        assertThrows(DataAccessResourceFailureException.class, () -> service.record(7L));
        verify(writer, times(2)).recordDailyActivityIfAbsent(7L, day);
    }

    @Test
    void existingDayDoesNotWriteAgainAndDeletedUserDoesNotProduceActivity() {
        var repository = mock(StatisticsRepository.class);
        var writer = new ActivityWriter(repository);
        var day = LocalDate.of(2026, 11, 1);
        when(repository.hasActivity(7L, day)).thenReturn(true);
        assertFalse(writer.recordDailyActivityIfAbsent(7L, day));
        verify(repository, never()).insertActivity(7L, day);
        when(repository.insertActivity(8L, day)).thenReturn(false);
        assertFalse(writer.recordDailyActivityIfAbsent(8L, day));
    }
}
