package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.labour.LabourRequest;
import com.madukotawatte.erp.dto.labour.LabourResponse;
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
@RequestMapping("/api/v1/labour")
@RequiredArgsConstructor
public class LabourController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LabourResponse> createLabour(@Valid @RequestBody LabourRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createLabour(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LabourResponse> getLabour(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getLabour(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<LabourResponse>> getAllLabour(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllLabour(pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<LabourResponse>> getLabourByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(dailyOperationsService.getLabourByDateRange(from, to));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LabourResponse> updateLabour(
            @PathVariable String id, @Valid @RequestBody LabourRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateLabour(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLabour(@PathVariable String id) {
        dailyOperationsService.deleteLabour(id);
        return ResponseEntity.noContent().build();
    }
}
