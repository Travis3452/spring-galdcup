package com.example.galdcup.statistics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.galdcup.statistics.StatisticsData.Totals;

@Repository
public class StatisticsRepository {
    public static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final NamedParameterJdbcTemplate jdbc;

    public StatisticsRepository(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(5);
        this.jdbc = new NamedParameterJdbcTemplate(template);
    }

    public Totals totals() {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM users) AS users,
                       (SELECT COUNT(*) FROM boards) AS boards,
                       (SELECT COUNT(*) FROM posts) AS posts,
                       (SELECT COUNT(*) FROM comments WHERE deleted_at IS NULL) AS comments
                FROM dual
                """, Map.of(), (rs, row) -> new Totals(rs.getLong("users"), rs.getLong("boards"),
                rs.getLong("posts"), rs.getLong("comments")));
    }

    public record ActivityCounts(long dau, long wau, long mau) { }

    public ActivityCounts activeUsers(LocalDate today) {
        var parameters = new MapSqlParameterSource()
                .addValue("today", Date.valueOf(today), Types.DATE)
                .addValue("week", Date.valueOf(today.minusDays(6)), Types.DATE)
                .addValue("month", Date.valueOf(today.minusDays(29)), Types.DATE)
                .addValue("until", Date.valueOf(today.plusDays(1)), Types.DATE);
        return jdbc.queryForObject("""
                SELECT COUNT(DISTINCT CASE WHEN activity_date >= :today THEN user_id END) AS dau,
                       COUNT(DISTINCT CASE WHEN activity_date >= :week THEN user_id END) AS wau,
                       COUNT(DISTINCT user_id) AS mau
                FROM user_daily_activity WHERE activity_date >= :month AND activity_date < :until
                """, parameters, (rs, row) -> new ActivityCounts(rs.getLong("dau"), rs.getLong("wau"), rs.getLong("mau")));
    }

    public LocalDate firstActivityDate() {
        return jdbc.queryForObject("SELECT MIN(activity_date) FROM user_daily_activity", Map.of(),
                (rs, row) -> rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate());
    }

    public LocalDate firstSignupDate() {
        String day = "TO_CHAR(MIN(created_at) AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD')";
        return jdbc.queryForObject("SELECT " + day + " FROM user_signup_events", Map.of(),
                (rs, row) -> rs.getString(1) == null ? null : LocalDate.parse(rs.getString(1)));
    }

    public Map<LocalDate, Long> signups(LocalDate from, LocalDate until) {
        String day = "TO_CHAR(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD')";
        var parameters = new MapSqlParameterSource();
        parameters.addValue("from", from.atStartOfDay(KOREA).toOffsetDateTime(), Types.TIMESTAMP_WITH_TIMEZONE);
        parameters.addValue("until", until.atStartOfDay(KOREA).toOffsetDateTime(), Types.TIMESTAMP_WITH_TIMEZONE);
        return jdbc.query("SELECT " + day + " AS signup_day, COUNT(*) AS signups FROM user_signup_events "
                        + "WHERE created_at >= :from AND created_at < :until GROUP BY " + day + " ORDER BY signup_day",
                parameters, rs -> {
                    Map<LocalDate, Long> values = new LinkedHashMap<>();
                    while (rs.next()) values.put(LocalDate.parse(rs.getString("signup_day")), rs.getLong("signups"));
                    return values;
                });
    }

    public boolean hasActivity(long userId, LocalDate day) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM user_daily_activity WHERE user_id=:user AND activity_date=:day",
                activityParameters(userId, day), Long.class);
        return count != null && count > 0;
    }

    public boolean insertActivity(long userId, LocalDate day) {
        return jdbc.update("INSERT INTO user_daily_activity (user_id, activity_date) "
                        + "SELECT :user, :day FROM users WHERE id=:user", activityParameters(userId, day)) == 1;
    }

    private MapSqlParameterSource activityParameters(long userId, LocalDate day) {
        return new MapSqlParameterSource().addValue("user", userId)
                .addValue("day", Date.valueOf(day), Types.DATE);
    }
}
