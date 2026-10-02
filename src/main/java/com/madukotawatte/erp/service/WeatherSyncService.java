package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.weather.WeatherDailyResponse;
import com.madukotawatte.erp.dto.weather.WeatherSyncResponse;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.repository.WeatherDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps {@code weather_daily} in sync with Open-Meteo.
 * Days up to {@value #ARCHIVE_LAG_DAYS} days old come from the archive API (final ERA5 data); newer days come from the
 * forecast API with {@code past_days} and are replaced by archive data on a later sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherSyncService {

    static final int ARCHIVE_LAG_DAYS = 5;
    private static final int MAX_FORECAST_PAST_DAYS = 92;
    private static final int RECENT_SYNC_DAYS = 10;

    private final OpenMeteoClient client;
    private final WeatherDailyRepository repository;

    @Value("${app.estate.timezone}") private String timezone;
    @Value("${app.weather.startup-backfill-days:730}") private int startupBackfillDays;
    @Value("${app.weather.startup-backfill-enabled:true}") private boolean startupBackfillEnabled;

    public LocalDate today() {
        return LocalDate.now(ZoneId.of(timezone));
    }

    /** Backfill / refresh an inclusive date range. */
    public WeatherSyncResponse sync(LocalDate from, LocalDate to) {
        LocalDate today = today();
        if (to == null || to.isAfter(today)) {
            to = today;
        }
        if (from == null) {
            from = to.minusDays(RECENT_SYNC_DAYS);
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        if (from.isBefore(LocalDate.of(1940, 1, 1))) {
            throw new BadRequestException("'from' is before the start of the archive (1940)");
        }

        int archiveCount = 0;
        int forecastCount = 0;
        try {
            LocalDate archiveEnd = today.minusDays(ARCHIVE_LAG_DAYS);
            LocalDate archiveTo = to.isBefore(archiveEnd) ? to : archiveEnd;
            // archive, chunked per calendar year
            LocalDate cursor = from;
            while (!cursor.isAfter(archiveTo)) {
                LocalDate chunkEnd = LocalDate.of(cursor.getYear(), 12, 31);
                if (chunkEnd.isAfter(archiveTo)) {
                    chunkEnd = archiveTo;
                }
                for (WeatherDay d : client.fetchArchive(cursor, chunkEnd)) {
                    if (d.precipitationMm() != null) {
                        repository.upsert(d, "archive");
                        archiveCount++;
                    }
                }
                cursor = chunkEnd.plusDays(1);
            }
            // recent days via the forecast API (past_days)
            LocalDate forecastFrom = from.isAfter(archiveEnd.plusDays(1)) ? from : archiveEnd.plusDays(1);
            if (!forecastFrom.isAfter(to)) {
                int pastDays = (int) java.time.temporal.ChronoUnit.DAYS.between(forecastFrom, today);
                if (pastDays > MAX_FORECAST_PAST_DAYS) {
                    pastDays = MAX_FORECAST_PAST_DAYS;
                }
                LocalDate lowest = today.minusDays(pastDays);
                for (WeatherDay d : client.fetchForecast(pastDays, 1)) {
                    if (d.date().isBefore(forecastFrom) || d.date().isAfter(to) || d.date().isBefore(lowest)) {
                        continue;
                    }
                    if (d.precipitationMm() != null) {
                        repository.upsert(d, "forecast");
                        forecastCount++;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Weather provider unavailable: " + e.getMessage(), e);
        }
        log.info("Weather sync {}..{}: {} archive days, {} forecast days", from, to, archiveCount, forecastCount);
        return new WeatherSyncResponse(from, to, archiveCount + forecastCount, archiveCount, forecastCount);
    }

    public List<WeatherDailyResponse> history(LocalDate from, LocalDate to) {
        return repository.findBetween(from, to).stream()
                .map(r -> new WeatherDailyResponse(r.date(), r.precipitationMm(), r.rainHours(), r.morningRainMm(),
                        r.morningRainProbabilityMax(), r.tempMax(), r.tempMin(), r.humidityMean(), r.windMax(),
                        r.et0Mm(), r.source()))
                .toList();
    }

    @Scheduled(cron = "0 30 6 * * *", zone = "${app.estate.timezone}")
    public void dailySync() {
        try {
            LocalDate today = today();
            sync(today.minusDays(RECENT_SYNC_DAYS), today);
        } catch (RuntimeException e) {
            log.warn("Scheduled weather sync failed: {}", e.getMessage());
        }
    }

    /** On startup, fill any gaps in the last N days. Runs on its own thread and never fails startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        if (!startupBackfillEnabled) {
            return;
        }
        Thread t = new Thread(this::backfillMissing, "weather-startup-backfill");
        t.setDaemon(true);
        t.start();
    }

    void backfillMissing() {
        try {
            LocalDate today = today();
            LocalDate start = today.minusDays(startupBackfillDays);
            Set<LocalDate> have = new HashSet<>(repository.findDatesBetween(start, today));
            LocalDate firstMissing = null;
            for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
                if (!have.contains(d)) {
                    firstMissing = d;
                    break;
                }
            }
            // always refresh the recent window so forecast-sourced rows get replaced by archive data
            LocalDate from = today.minusDays(RECENT_SYNC_DAYS);
            if (firstMissing != null && firstMissing.isBefore(from)) {
                from = firstMissing;
            }
            log.info("Weather startup sync from {} to {}", from, today);
            sync(from, today);
        } catch (Exception e) {
            log.warn("Weather startup backfill skipped: {}", e.getMessage());
        }
    }
}
