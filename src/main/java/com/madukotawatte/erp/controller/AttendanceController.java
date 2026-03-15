package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.attendance.AttendanceBulkRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.service.DailyOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<AttendanceResponse> createAttendance(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createAttendance(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AttendanceResponse>> createBulkAttendance(@Valid @RequestBody AttendanceBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createAttendanceBulk(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<AttendanceResponse> getAttendance(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getAttendance(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<AttendanceResponse>> getAllAttendances(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllAttendances(pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(dailyOperationsService.getAttendanceByDateRange(from, to));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable String id, @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateAttendance(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAttendance(@PathVariable String id) {
        dailyOperationsService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
