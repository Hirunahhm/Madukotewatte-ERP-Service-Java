package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse;
import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse.Day;
import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse.YieldBasis;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 7-day tapping outlook from the live forecast (cached ~30 minutes).
 * A day is predicted tappable when morning (04:00-09:00) rain is below the threshold AND the maximum
 * morning rain probability is below the probability limit.
 */
@Service
@RequiredArgsConstructor
public class WeatherOutlookService {

    static final int OUTLOOK_DAYS = 7;
    static final int YIELD_WINDOW_DAYS = 60;
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final OpenMeteoClient client;
    private final WeatherInsightsService insightsService;
    private final WeatherSyncService syncService;

    @Value("${app.weather.tap-rain-threshold-mm:1.0}") private BigDecimal thresholdMm;
    @Value("${app.weather.tap-rain-probability-max:60}") private BigDecimal probabilityLimit;

    private record Cached(Instant at, LocalDate forDate, List<WeatherDay> days) {
    }

    private volatile Cached cache;

    public WeatherOutlookResponse outlook() {
        LocalDate today = syncService.today();
        Cached c = cache;
        if (c == null || !c.forDate().equals(today) || Duration.between(c.at(), Instant.now()).compareTo(CACHE_TTL) > 0) {
            try {
                c = new Cached(Instant.now(), today, client.fetchForecast(0, OUTLOOK_DAYS));
                cache = c;
            } catch (IOException e) {
                if (c == null) {
                    throw new IllegalStateException("Weather provider unavailable: " + e.getMessage(), e);
                }
                // serve stale data rather than failing
            }
        }

        var yield = insightsService.averageYield(today.minusDays(YIELD_WINDOW_DAYS), today.minusDays(1));
        List<Day> out = new ArrayList<>();
        int tappable = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (WeatherDay w : c.days()) {
            if (w.date().isBefore(today) || out.size() >= OUTLOOK_DAYS) {
                continue;
            }
            boolean ok = isTappable(w);
            BigDecimal expected = ok ? yield.average() : BigDecimal.ZERO;
            if (ok) {
                tappable++;
                if (expected != null) {
                    total = total.add(expected);
                }
            }
            out.add(new Day(w.date(), w.precipitationMm(), w.morningRainMm(), w.morningRainProbabilityMax(),
                    w.tempMax(), w.tempMin(), ok, expected, summary(w, ok)));
        }

        String basis = yield.sampleDays() == 0
                ? "No latex records in the last " + YIELD_WINDOW_DAYS + " days, so expected latex cannot be estimated."
                : "Average latex per tapping day over the last " + YIELD_WINDOW_DAYS + " days (" + yield.sampleDays()
                + " tapping days) x predicted tappable days.";
        return new WeatherOutlookResponse(c.at(), thresholdMm, probabilityLimit,
                new YieldBasis(basis, yield.average(), yield.sampleDays(), YIELD_WINDOW_DAYS),
                tappable, yield.average() == null ? null : total, WeatherInsightsService.LATEX_UNIT, out);
    }

    boolean isTappable(WeatherDay w) {
        BigDecimal morning = w.morningRainMm() == null ? BigDecimal.ZERO : w.morningRainMm();
        boolean probOk = w.morningRainProbabilityMax() == null
                || w.morningRainProbabilityMax().compareTo(probabilityLimit) < 0;
        return morning.compareTo(thresholdMm) < 0 && probOk;
    }

    private String summary(WeatherDay w, boolean ok) {
        String morning = w.morningRainMm() == null ? "n/a" : w.morningRainMm().stripTrailingZeros().toPlainString() + " mm";
        String prob = w.morningRainProbabilityMax() == null ? "" : ", " + w.morningRainProbabilityMax().intValue() + "% chance";
        String day = w.precipitationMm() == null ? "n/a" : w.precipitationMm().stripTrailingZeros().toPlainString() + " mm";
        if (ok) {
            return "Good tapping morning (" + morning + " rain 4-9am" + prob + "); " + day + " rain for the day.";
        }
        return "Likely no tapping: " + morning + " rain expected 4-9am" + prob + "; " + day + " rain for the day.";
    }
}
