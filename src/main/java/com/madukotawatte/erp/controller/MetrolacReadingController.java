package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingRequest;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingResponse;
import com.madukotawatte.erp.service.DailyOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metrolac-readings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class MetrolacReadingController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    public ResponseEntity<MetrolacReadingResponse> createMetrolacReading(@Valid @RequestBody MetrolacReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createMetrolacReading(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetrolacReadingResponse> getMetrolacReading(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getMetrolacReading(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MetrolacReadingResponse>> getAllMetrolacReadings(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllMetrolacReadings(pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<MetrolacReadingResponse>> getMetrolacReadingsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(dailyOperationsService.getMetrolacReadingsByLoad(loadId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetrolacReadingResponse> updateMetrolacReading(
            @PathVariable String id, @Valid @RequestBody MetrolacReadingRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateMetrolacReading(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMetrolacReading(@PathVariable String id) {
        dailyOperationsService.deleteMetrolacReading(id);
        return ResponseEntity.noContent().build();
    }
}
