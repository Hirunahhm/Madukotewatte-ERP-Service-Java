package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.service.WeatherDay;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/** JDBC access to {@code weather_daily} (no JPA entity: the table is a pure read/upsert store). */
@Repository
@RequiredArgsConstructor
public class WeatherDailyRepository {

    private final JdbcTemplate jdbc;

    /**
     * Upsert by date. Archive data always wins; forecast data only inserts new rows or refreshes
     * rows that are themselves forecast-sourced.
     */
    public void upsert(WeatherDay d, String source) {
        jdbc.update("""
                INSERT INTO weather_daily (weather_date, precipitation_mm, rain_hours, morning_rain_mm,
                    morning_rain_probability_max, temp_max, temp_min, humidity_mean, wind_max, et0_mm, source, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (weather_date) DO UPDATE SET
                    precipitation_mm = EXCLUDED.precipitation_mm,
                    rain_hours = EXCLUDED.rain_hours,
                    morning_rain_mm = EXCLUDED.morning_rain_mm,
                    morning_rain_probability_max = COALESCE(EXCLUDED.morning_rain_probability_max, weather_daily.morning_rain_probability_max),
                    temp_max = EXCLUDED.temp_max,
                    temp_min = EXCLUDED.temp_min,
                    humidity_mean = EXCLUDED.humidity_mean,
                    wind_max = EXCLUDED.wind_max,
                    et0_mm = EXCLUDED.et0_mm,
                    source = EXCLUDED.source,
                    fetched_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE EXCLUDED.source = 'archive' OR weather_daily.source = 'forecast'
                """,
                Date.valueOf(d.date()), d.precipitationMm(), d.rainHours(), d.morningRainMm(),
                d.morningRainProbabilityMax(), d.tempMax(), d.tempMin(), d.humidityMean(), d.windMax(), d.et0Mm(),
                source);
    }

    public List<WeatherRow> findBetween(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT weather_date, precipitation_mm, rain_hours, morning_rain_mm, morning_rain_probability_max,
                       temp_max, temp_min, humidity_mean, wind_max, et0_mm, source
                FROM weather_daily WHERE weather_date BETWEEN ? AND ? ORDER BY weather_date
                """,
                (rs, i) -> new WeatherRow(
                        rs.getDate(1).toLocalDate(), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4),
                        rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                        rs.getBigDecimal(9), rs.getBigDecimal(10), rs.getString(11)),
                Date.valueOf(from), Date.valueOf(to));
    }

    public List<LocalDate> findDatesBetween(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT weather_date FROM weather_daily WHERE weather_date BETWEEN ? AND ? ORDER BY weather_date",
                (rs, i) -> rs.getDate(1).toLocalDate(), Date.valueOf(from), Date.valueOf(to));
    }

    public record WeatherRow(LocalDate date, BigDecimal precipitationMm, BigDecimal rainHours,
                             BigDecimal morningRainMm, BigDecimal morningRainProbabilityMax,
                             BigDecimal tempMax, BigDecimal tempMin, BigDecimal humidityMean,
                             BigDecimal windMax, BigDecimal et0Mm, String source) {
    }
}
