package com.example.galdcup.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.Types;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.galdcup.statistics.StatisticsData.Totals;

@Repository
public class StatisticsRepository {
    public static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final NamedParameterJdbcTemplate jdbc;
    private final boolean oracle;

    @Autowired
    public StatisticsRepository(DataSource dataSource, @Value("${spring.jpa.database-platform}") String dialect) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(5);
        this.jdbc = new NamedParameterJdbcTemplate(template);
        this.oracle = dialect.contains("Oracle");
        if (!oracle && !dialect.contains("MySQL")) throw new IllegalArgumentException("Unsupported statistics database");
    }

    StatisticsRepository(NamedParameterJdbcTemplate jdbc, boolean oracle) {
        this.jdbc = jdbc;
        this.oracle = oracle;
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
        String day = oracle ? "TO_CHAR(MIN(created_at) AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD')"
                : "DATE_FORMAT(CONVERT_TZ(MIN(created_at), '+00:00', '+09:00'), '%Y-%m-%d')";
        return jdbc.queryForObject("SELECT " + day + " FROM user_signup_events", Map.of(),
                (rs, row) -> rs.getString(1) == null ? null : LocalDate.parse(rs.getString(1)));
    }

    public Map<LocalDate, Long> signups(LocalDate from, LocalDate until) {
        String day = oracle ? "TO_CHAR(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD')"
                : "DATE_FORMAT(CONVERT_TZ(created_at, '+00:00', '+09:00'), '%Y-%m-%d')";
        var parameters = new MapSqlParameterSource();
        if (oracle) {
            parameters.addValue("from", from.atStartOfDay(KOREA).toOffsetDateTime(), Types.TIMESTAMP_WITH_TIMEZONE);
            parameters.addValue("until", until.atStartOfDay(KOREA).toOffsetDateTime(), Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            parameters.addValue("from", LocalDateTime.ofInstant(from.atStartOfDay(KOREA).toInstant(), ZoneOffset.UTC), Types.TIMESTAMP);
            parameters.addValue("until", LocalDateTime.ofInstant(until.atStartOfDay(KOREA).toInstant(), ZoneOffset.UTC), Types.TIMESTAMP);
        }
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
