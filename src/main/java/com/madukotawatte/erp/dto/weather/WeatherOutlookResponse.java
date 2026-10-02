package com.madukotawatte.erp.dto.weather;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeatherOutlookResponse(
        Instant fetchedAt,
        BigDecimal tapRainThresholdMm,
        BigDecimal tapRainProbabilityMax,
        YieldBasis basis,
        int tappableDays,
        BigDecimal expectedLatexTotal,
        String latexUnit,
        List<Day> days
) {

    public record YieldBasis(String description, BigDecimal avgLatexPerTappingDay, int sampleDays, int windowDays) {
    }

    public record Day(
            LocalDate date,
            BigDecimal rainMm,
            BigDecimal morningRainMm,
            BigDecimal morningRainProbabilityMax,
            BigDecimal tempMax,
            BigDecimal tempMin,
            boolean tappable,
            BigDecimal expectedLatex,
            String summary
    ) {
    }
}
