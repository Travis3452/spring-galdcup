package com.example.galdcup.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class StatisticsData {
    private StatisticsData() { }
    public record Totals(long users, long boards, long posts, long comments) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ActiveUsers(Long dau, Long wau, Long mau, LocalDate firstRecordedDate,
                              boolean dailyPartial, boolean weeklyPartial, boolean monthlyPartial) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SignupPoint(LocalDate date, Long count) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Signups(LocalDate fromDate, LocalDate toDate, LocalDate firstRecordedDate,
                          Long total, boolean partial, List<SignupPoint> points) { }
    public record Dashboard(Instant generatedAt, LocalDate asOfDate, String timezone,
                            Totals totals, ActiveUsers activeUsers, Signups signups) { }
}
