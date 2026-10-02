package com.madukotawatte.erp.dto.weather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Weather joined with estate activity. Latex quantity is the sum of {@code latex_records.latex_amount}
 * (litres of field latex, as recorded) by the record's date; dry-rubber weight is not derivable per day.
 */
public record WeatherInsightsResponse(
        LocalDate from,
        LocalDate to,
        String latexUnit,
        List<Day> days,
        List<Month> months,
        Correlations correlations,
        WetDryYield morningYield,
        YearToDate rainfallYearToDate,
        Integer rainLostTappingDaysThisMonth,
        String thisMonth
) {

    public record Day(
            LocalDate date,
            BigDecimal rainfallMm,
            BigDecimal morningRainMm,
            BigDecimal latexQty,
            int tappers,
            int treesTapped,
            boolean tapped,
            boolean rainLost
    ) {
    }

    public record Month(
            String month,
            BigDecimal rainfallMm,
            int rainDays,
            int tappingDays,
            int rainLostDays,
            BigDecimal latexQty,
            BigDecimal avgLatexPerTappingDay,
            long treesTapped
    ) {
    }

    /** Pearson coefficients; null when there is not enough variation or fewer than 3 samples. */
    public record Correlations(
            BigDecimal rainfallVsYield,
            int rainfallVsYieldSamples,
            BigDecimal morningRainVsTapping,
            int morningRainVsTappingSamples,
            String rainfallVsYieldBasis,
            String morningRainVsTappingBasis
    ) {
    }

    public record WetDryYield(
            BigDecimal thresholdMm,
            BigDecimal avgLatexDryMornings,
            int dryMorningTappingDays,
            BigDecimal avgLatexWetMornings,
            int wetMorningTappingDays
    ) {
    }

    public record YearToDate(
            int year,
            BigDecimal thisYearMm,
            BigDecimal lastYearSamePeriodMm,
            BigDecimal differenceMm,
            BigDecimal differencePct,
            int thisYearDaysWithData,
            int lastYearDaysWithData
    ) {
    }
}
