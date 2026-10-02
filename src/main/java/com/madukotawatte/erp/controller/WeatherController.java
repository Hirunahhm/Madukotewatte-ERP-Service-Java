package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.weather.WeatherDailyResponse;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse;
import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse;
import com.madukotawatte.erp.dto.weather.WeatherSyncResponse;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.service.WeatherInsightsService;
import com.madukotawatte.erp.service.WeatherOutlookService;
import com.madukotawatte.erp.service.WeatherSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherSyncService syncService;
    private final WeatherInsightsService insightsService;
    private final WeatherOutlookService outlookService;

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeatherSyncResponse> sync(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(syncService.sync(from, to));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<WeatherDailyResponse>> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        return ResponseEntity.ok(syncService.history(from, to));
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<WeatherInsightsResponse> insights(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(insightsService.insights(from, to));
    }

    @GetMapping("/outlook")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<WeatherOutlookResponse> outlook() {
        return ResponseEntity.ok(outlookService.outlook());
    }
}
