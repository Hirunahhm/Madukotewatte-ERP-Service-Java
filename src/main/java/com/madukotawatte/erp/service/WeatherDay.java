package com.madukotawatte.erp.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of weather as returned by Open-Meteo (internal transfer object). */
public record WeatherDay(
        LocalDate date,
        BigDecimal precipitationMm,
        BigDecimal rainHours,
        BigDecimal morningRainMm,
        BigDecimal morningRainProbabilityMax,
        BigDecimal tempMax,
        BigDecimal tempMin,
        BigDecimal humidityMean,
        BigDecimal windMax,
        BigDecimal et0Mm
) {
}
