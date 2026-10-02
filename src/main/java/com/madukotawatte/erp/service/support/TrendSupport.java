package com.madukotawatte.erp.service.support;

import com.madukotawatte.erp.dto.finance.TrendPointResponse;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Shared week / month / year bucketing used by the sales, expense and account trend endpoints. */
public final class TrendSupport {

    private TrendSupport() {}

    public record Window(LocalDate from, LocalDate to) {}

    public static Window resolveWindow(String scale) {
        LocalDate today = LocalDate.now();
        return switch (scale) {
            case "week" -> new Window(today.with(DayOfWeek.MONDAY), today.with(DayOfWeek.MONDAY).plusDays(6));
            case "year" -> new Window(today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31));
            default -> new Window(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1).minusDays(1));
        };
    }

    public static List<String> labels(String scale) {
        return switch (scale) {
            case "week" -> List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
            case "year" -> List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
            default -> List.of("W1", "W2", "W3", "W4");
        };
    }

    /** Last day of every bucket, in label order. */
    public static List<LocalDate> bucketEndDates(String scale, Window window) {
        return switch (scale) {
            case "week" -> IntStream.range(0, 7).mapToObj(window.from()::plusDays).collect(Collectors.toList());
            case "year" -> IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> LocalDate.of(window.from().getYear(), m, 1).plusMonths(1).minusDays(1))
                    .collect(Collectors.toList());
            default -> List.of(
                    window.from().plusDays(6),
                    window.from().plusDays(13),
                    window.from().plusDays(20),
                    window.to());
        };
    }

    public static String bucketLabel(String scale, LocalDate date) {
        return switch (scale) {
            case "week" -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            case "year" -> date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            default -> "W" + Math.min(4, ((date.getDayOfMonth() - 1) / 7) + 1);
        };
    }

    /** rows are [date, amount]; sums amounts into the scale's buckets. */
    public static List<TrendPointResponse> bucketSums(String scale, List<Object[]> rows) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String label : labels(scale)) totals.put(label, BigDecimal.ZERO);
        for (Object[] row : rows) {
            totals.merge(bucketLabel(scale, toLocalDate(row[0])), (BigDecimal) row[1], BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> TrendPointResponse.builder().name(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());
    }

    public static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        throw new IllegalStateException("Unexpected date type: " + value.getClass());
    }
}
