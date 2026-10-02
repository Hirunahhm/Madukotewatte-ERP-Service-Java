package com.madukotawatte.erp.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only queries that summarise estate activity per day, to be joined with weather.
 * Native SQL over existing tables so no other service is touched.
 */
@Repository
@RequiredArgsConstructor
public class WeatherEstateActivityRepository {

    private final JdbcTemplate jdbc;

    /** Litres of latex recorded per day (sum of latex_records.latex_amount by record date). */
    public List<LatexDay> latexPerDay(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT CAST(timestamp AS DATE) AS d, SUM(latex_amount)
                FROM latex_records
                WHERE timestamp >= ? AND timestamp < ?
                GROUP BY 1
                """,
                (rs, i) -> new LatexDay(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2)),
                Date.valueOf(from), Date.valueOf(to.plusDays(1)));
    }

    /**
     * Tapping attendance per day: rows with no_work='none' and no_of_trees>0 count as tapping;
     * no_work='rain' marks a rain-lost day. The day is the calendar date when linked, else the timestamp date.
     */
    public List<AttendanceDay> attendancePerDay(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT COALESCE(c.calendar_date, CAST(a.timestamp AS DATE)) AS d,
                       COUNT(*) FILTER (WHERE a.no_work = 'none' AND COALESCE(a.no_of_trees, 0) > 0) AS tappers,
                       COALESCE(SUM(a.no_of_trees) FILTER (WHERE a.no_work = 'none' AND COALESCE(a.no_of_trees, 0) > 0), 0) AS trees,
                       COALESCE(BOOL_OR(a.no_work = 'rain'), FALSE) AS rain_att
                FROM attendance a
                LEFT JOIN calendar c ON c.calendar_id = a.calendar_id
                WHERE COALESCE(c.calendar_date, CAST(a.timestamp AS DATE)) BETWEEN ? AND ?
                GROUP BY 1
                """,
                (rs, i) -> new AttendanceDay(rs.getDate(1).toLocalDate(), rs.getInt(2), rs.getLong(3), rs.getBoolean(4)),
                Date.valueOf(from), Date.valueOf(to));
    }

    public List<LocalDate> rainedCalendarDays(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT calendar_date FROM calendar WHERE rained = TRUE AND calendar_date BETWEEN ? AND ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), Date.valueOf(from), Date.valueOf(to));
    }

    public record LatexDay(LocalDate date, BigDecimal litres) {
    }

    public record AttendanceDay(LocalDate date, int tappers, long trees, boolean rainAttendance) {
    }
}
