package com.madukotawatte.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Thin Open-Meteo client (no API key). The morning (tapping) window is the hourly slots 04:00-08:00 local,
 * i.e. precipitation falling between 04:00 and 09:00.
 */
@Component
public class OpenMeteoClient {

    static final int MORNING_START_HOUR = 4;
    static final int MORNING_END_HOUR_EXCLUSIVE = 9;

    private static final String DAILY_VARS =
            "precipitation_sum,precipitation_hours,temperature_2m_max,temperature_2m_min,"
                    + "wind_speed_10m_max,et0_fao_evapotranspiration";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${app.estate.latitude}") private double latitude;
    @Value("${app.estate.longitude}") private double longitude;
    @Value("${app.estate.timezone}") private String timezone;
    @Value("${app.weather.forecast-url}") private String forecastUrl;
    @Value("${app.weather.archive-url}") private String archiveUrl;

    /** Forecast API: {@code pastDays} (max 92) days back plus {@code forecastDays} days from today. */
    public List<WeatherDay> fetchForecast(int pastDays, int forecastDays) throws IOException {
        String url = forecastUrl + "?" + common()
                + "&past_days=" + Math.min(Math.max(pastDays, 0), 92)
                + "&forecast_days=" + Math.min(Math.max(forecastDays, 1), 16)
                + "&hourly=precipitation,precipitation_probability,relative_humidity_2m"
                + "&daily=" + DAILY_VARS;
        return parse(get(url));
    }

    /** Archive (ERA5) API for an inclusive date range. */
    public List<WeatherDay> fetchArchive(LocalDate from, LocalDate to) throws IOException {
        String url = archiveUrl + "?" + common()
                + "&start_date=" + from + "&end_date=" + to
                + "&hourly=precipitation,relative_humidity_2m"
                + "&daily=" + DAILY_VARS;
        return parse(get(url));
    }

    private String common() {
        return "latitude=" + latitude + "&longitude=" + longitude
                + "&timezone=" + timezone.replace("/", "%2F")
                + "&wind_speed_unit=kmh";
    }

    private JsonNode get(String url) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(60)).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    return objectMapper.readTree(res.body());
                }
                last = new IOException("Open-Meteo returned HTTP " + res.statusCode() + ": " + abbreviate(res.body()));
                if (res.statusCode() != 429 && res.statusCode() < 500) {
                    throw last;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while calling Open-Meteo", e);
            } catch (IOException e) {
                last = e;
            }
            try {
                Thread.sleep(2000L * attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while calling Open-Meteo", e);
            }
        }
        throw last;
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 200 ? s.substring(0, 200) : s;
    }

    private List<WeatherDay> parse(JsonNode root) {
        JsonNode daily = root.path("daily");
        JsonNode hourly = root.path("hourly");
        // per date: [morningRainSum, morningProbMax, humiditySum, humidityCount]
        Map<LocalDate, BigDecimal[]> hourAgg = new TreeMap<>();

        JsonNode hTime = hourly.path("time");
        for (int i = 0; i < hTime.size(); i++) {
            LocalDateTime t = LocalDateTime.parse(hTime.get(i).asText());
            BigDecimal[] agg = hourAgg.computeIfAbsent(t.toLocalDate(),
                    d -> new BigDecimal[]{null, null, BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal hum = num(hourly.path("relative_humidity_2m"), i);
            if (hum != null) {
                agg[2] = agg[2].add(hum);
                agg[3] = agg[3].add(BigDecimal.ONE);
            }
            if (t.getHour() >= MORNING_START_HOUR && t.getHour() < MORNING_END_HOUR_EXCLUSIVE) {
                BigDecimal p = num(hourly.path("precipitation"), i);
                if (p != null) {
                    agg[0] = agg[0] == null ? p : agg[0].add(p);
                }
                BigDecimal prob = num(hourly.path("precipitation_probability"), i);
                if (prob != null && (agg[1] == null || prob.compareTo(agg[1]) > 0)) {
                    agg[1] = prob;
                }
            }
        }

        List<WeatherDay> out = new ArrayList<>();
        JsonNode dTime = daily.path("time");
        for (int i = 0; i < dTime.size(); i++) {
            LocalDate date = LocalDate.parse(dTime.get(i).asText());
            BigDecimal[] agg = hourAgg.get(date);
            BigDecimal humidity = null;
            if (agg != null && agg[3].signum() > 0) {
                humidity = agg[2].divide(agg[3], 1, RoundingMode.HALF_UP);
            }
            out.add(new WeatherDay(
                    date,
                    num(daily.path("precipitation_sum"), i),
                    num(daily.path("precipitation_hours"), i),
                    agg == null || agg[0] == null ? null : agg[0].setScale(2, RoundingMode.HALF_UP),
                    agg == null ? null : agg[1],
                    num(daily.path("temperature_2m_max"), i),
                    num(daily.path("temperature_2m_min"), i),
                    humidity,
                    num(daily.path("wind_speed_10m_max"), i),
                    num(daily.path("et0_fao_evapotranspiration"), i)));
        }
        return out;
    }

    private static BigDecimal num(JsonNode array, int i) {
        if (!array.isArray() || i >= array.size() || array.get(i).isNull()) {
            return null;
        }
        return BigDecimal.valueOf(array.get(i).asDouble());
    }
}
