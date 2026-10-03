package com.madukotawatte.erp.service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

/** Date-range math for the dashboard (calendar year and Sri Lankan financial year, 1 Apr - 31 Mar). */
public final class DashboardPeriods {

    private DashboardPeriods() {
    }

    public record Range(LocalDate from, LocalDate to) {
    }

    /** First day of the financial year containing the date. */
    public static LocalDate financialYearStart(LocalDate date) {
        int year = date.getMonthValue() >= Month.APRIL.getValue() ? date.getYear() : date.getYear() - 1;
        return LocalDate.of(year, Month.APRIL, 1);
    }

    /** "2026/27" style label of the financial year containing the date. */
    public static String financialYearLabel(LocalDate date) {
        LocalDate start = financialYearStart(date);
        return start.getYear() + "/" + String.format("%02d", (start.getYear() + 1) % 100);
    }

    /** The reference day of a month: today when it is the current month, else the month's last day. */
    public static LocalDate asOf(YearMonth month, LocalDate today) {
        return month.equals(YearMonth.from(today)) ? today : month.atEndOfMonth();
    }

    public static Range monthToDate(YearMonth month, LocalDate today) {
        return new Range(month.atDay(1), asOf(month, today));
    }

    public static Range previousMonthFull(YearMonth month) {
        YearMonth prev = month.minusMonths(1);
        return new Range(prev.atDay(1), prev.atEndOfMonth());
    }

    /** Previous month limited to the same number of days elapsed (clamped to its length). */
    public static Range previousMonthSameDays(YearMonth month, LocalDate today) {
        YearMonth prev = month.minusMonths(1);
        int day = Math.min(asOf(month, today).getDayOfMonth(), prev.lengthOfMonth());
        return new Range(prev.atDay(1), prev.atDay(day));
    }

    public static Range calendarYearToDate(LocalDate asOf) {
        return new Range(LocalDate.of(asOf.getYear(), 1, 1), asOf);
    }

    public static Range financialYearToDate(LocalDate asOf) {
        return new Range(financialYearStart(asOf), asOf);
    }
}
