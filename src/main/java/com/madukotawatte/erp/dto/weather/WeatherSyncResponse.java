package com.madukotawatte.erp.dto.weather;

import java.time.LocalDate;

public record WeatherSyncResponse(LocalDate from, LocalDate to, int daysUpserted, int archiveDays, int forecastDays) {
}
