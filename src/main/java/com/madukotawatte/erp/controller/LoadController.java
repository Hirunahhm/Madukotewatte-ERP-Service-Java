package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.LoadRequest;
import com.madukotawatte.erp.dto.load.LoadResponse;
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

@RestController
@RequestMapping("/api/v1/loads")
@RequiredArgsConstructor
public class LoadController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LoadResponse> createLoad(@Valid @RequestBody LoadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createLoad(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LoadResponse> getLoad(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getLoad(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<LoadResponse>> getAllLoads(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllLoads(pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<LoadResponse>> getLoadsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getLoadsByDateRange(from, to, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<LoadResponse> updateLoad(@PathVariable String id, @Valid @RequestBody LoadRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateLoad(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLoad(@PathVariable String id) {
        dailyOperationsService.deleteLoad(id);
        return ResponseEntity.noContent().build();
    }
}
