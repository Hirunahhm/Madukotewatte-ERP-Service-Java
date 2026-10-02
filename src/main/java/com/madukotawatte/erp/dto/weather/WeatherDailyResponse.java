package com.madukotawatte.erp.dto.weather;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeatherDailyResponse(
        LocalDate date,
        BigDecimal precipitationMm,
        BigDecimal rainHours,
        BigDecimal morningRainMm,
        BigDecimal morningRainProbabilityMax,
        BigDecimal tempMax,
        BigDecimal tempMin,
        BigDecimal humidityMean,
        BigDecimal windMax,
        BigDecimal et0Mm,
        String source
) {
}
