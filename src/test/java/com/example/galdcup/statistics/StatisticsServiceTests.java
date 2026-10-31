package com.example.galdcup.statistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTests {
    private StatisticsRepository repository;
    private StatisticsService service;
    private final LocalDate today = LocalDate.of(2026, 11, 1);

    @BeforeEach
    void setUp() {
        repository = mock(StatisticsRepository.class);
        service = new StatisticsService(repository, Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC));
        when(repository.totals()).thenReturn(new StatisticsData.Totals(1006, 10, 100, 200));
        when(repository.activeUsers(today)).thenReturn(new StatisticsRepository.ActivityCounts(2, 5, 8));
        when(repository.signups(any(), any())).thenReturn(Map.of());
    }

    @Test
    void usesKoreanCalendarAndInclusiveSevenAndThirtyDayWindows() {
        when(repository.firstActivityDate()).thenReturn(today.minusDays(10));
        var result = service.dashboard(7);
        assertEquals(today, result.asOfDate());
        assertEquals(today.minusDays(6), result.signups().fromDate());
        assertEquals(7, result.signups().points().size());
        assertEquals(2L, result.activeUsers().dau());
        assertEquals(5L, result.activeUsers().wau());
        assertEquals(8L, result.activeUsers().mau());
        assertFalse(result.activeUsers().dailyPartial());
        assertFalse(result.activeUsers().weeklyPartial());
        assertTrue(result.activeUsers().monthlyPartial());
        verify(repository).activeUsers(today);
        verify(repository).signups(today.minusDays(6), today.plusDays(1));
    }

    @Test
    void missingCollectionHistoryIsNotRenderedAsMeasuredZero() {
        var result = service.dashboard(30);
        assertNull(result.activeUsers().dau());
        assertNull(result.activeUsers().wau());
        assertNull(result.activeUsers().mau());
        assertNull(result.signups().total());
        assertTrue(result.signups().partial());
        assertTrue(result.signups().points().stream().allMatch(p -> p.count() == null));
        assertEquals(1006, result.totals().users());
    }

    @Test
    void fillsOnlyObservedSignupPeriodAndKeepsOlderDatesUnknown() {
        LocalDate first = today.minusDays(2);
        when(repository.firstSignupDate()).thenReturn(first);
        when(repository.signups(any(), any())).thenReturn(Map.of(first, 3L, today, 2L));
        var result = service.dashboard(7).signups();
        assertNull(result.points().get(0).count());
        assertEquals(3L, result.points().get(4).count());
        assertEquals(0L, result.points().get(5).count());
        assertEquals(2L, result.points().get(6).count());
        assertEquals(5L, result.total());
        assertTrue(result.partial());
    }

    @Test
    void observedEmptyRecentPeriodIsZeroRatherThanMissing() {
        when(repository.firstSignupDate()).thenReturn(today.minusDays(90));
        when(repository.firstActivityDate()).thenReturn(today.minusDays(90));
        when(repository.activeUsers(today)).thenReturn(new StatisticsRepository.ActivityCounts(0, 0, 0));
        var result = service.dashboard(7);
        assertEquals(0L, result.signups().total());
        assertEquals(0L, result.activeUsers().dau());
        assertFalse(result.signups().partial());
        assertFalse(result.activeUsers().monthlyPartial());
    }

    @Test
    void cachesBoundedRangesButRejectsArbitraryLookbacks() {
        var first = service.dashboard(7);
        assertSame(first, service.dashboard(7));
        verify(repository, times(1)).totals();
        assertThrows(IllegalArgumentException.class, () -> service.dashboard(365));
        assertThrows(IllegalArgumentException.class, () -> service.dashboard(0));
    }
}
