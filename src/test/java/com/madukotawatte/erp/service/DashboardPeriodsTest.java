package com.madukotawatte.erp.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardPeriodsTest {

    @Test
    void financialYearStartsInApril() {
        assertEquals(LocalDate.of(2026, 4, 1), DashboardPeriods.financialYearStart(LocalDate.of(2026, 4, 1)));
        assertEquals(LocalDate.of(2026, 4, 1), DashboardPeriods.financialYearStart(LocalDate.of(2026, 10, 3)));
        assertEquals(LocalDate.of(2026, 4, 1), DashboardPeriods.financialYearStart(LocalDate.of(2027, 3, 31)));
        assertEquals(LocalDate.of(2025, 4, 1), DashboardPeriods.financialYearStart(LocalDate.of(2026, 3, 31)));
        assertEquals(LocalDate.of(2025, 4, 1), DashboardPeriods.financialYearStart(LocalDate.of(2026, 1, 15)));
    }

    @Test
    void financialYearLabelSpansTwoYears() {
        assertEquals("2026/27", DashboardPeriods.financialYearLabel(LocalDate.of(2026, 10, 3)));
        assertEquals("2025/26", DashboardPeriods.financialYearLabel(LocalDate.of(2026, 3, 31)));
        assertEquals("2099/00", DashboardPeriods.financialYearLabel(LocalDate.of(2099, 5, 1)));
    }

    @Test
    void currentMonthRunsToToday_pastMonthToItsEnd() {
        LocalDate today = LocalDate.of(2026, 10, 3);
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2026, 10, 1), today),
                DashboardPeriods.monthToDate(YearMonth.of(2026, 10), today));
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                DashboardPeriods.monthToDate(YearMonth.of(2026, 2), today));
    }

    @Test
    void previousMonthSameDaysIsClampedToMonthLength() {
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
                DashboardPeriods.previousMonthSameDays(YearMonth.of(2026, 3), LocalDate.of(2026, 3, 31)));
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)),
                DashboardPeriods.previousMonthSameDays(YearMonth.of(2026, 10), LocalDate.of(2026, 10, 3)));
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)),
                DashboardPeriods.previousMonthFull(YearMonth.of(2026, 1)));
    }

    @Test
    void yearToDateRanges() {
        LocalDate asOf = LocalDate.of(2026, 2, 14);
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2026, 1, 1), asOf), DashboardPeriods.calendarYearToDate(asOf));
        assertEquals(new DashboardPeriods.Range(LocalDate.of(2025, 4, 1), asOf), DashboardPeriods.financialYearToDate(asOf));
    }
}
