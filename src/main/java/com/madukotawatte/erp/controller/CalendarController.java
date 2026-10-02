package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.calendar.CalendarRequest;
import com.madukotawatte.erp.dto.calendar.CalendarResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<CalendarResponse> createOrUpdateCalendar(@Valid @RequestBody CalendarRequest request) {
        return ResponseEntity.ok(calendarService.createOrUpdateCalendar(request));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<CalendarResponse> getCalendarByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(calendarService.getCalendarByDate(date));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<CalendarResponse>> getAllCalendars(
            @PageableDefault(size = 31) Pageable pageable) {
        return ResponseEntity.ok(calendarService.getAllCalendars(pageable));
    }
}
