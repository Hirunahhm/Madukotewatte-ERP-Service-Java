package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.ammonia.AmmoniaBalanceResponse;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordRequest;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordResponse;
import com.madukotawatte.erp.dto.ammonia.AmmoniaUsageResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.service.ProductionService;
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
@RequestMapping("/api/v1/ammonia-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AmmoniaRecordController {

    private final ProductionService productionService;

    @PostMapping
    public ResponseEntity<AmmoniaRecordResponse> createAmmoniaRecord(@Valid @RequestBody AmmoniaRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productionService.createAmmoniaRecord(request));
    }

    @GetMapping("/balance")
    public ResponseEntity<AmmoniaBalanceResponse> getAmmoniaBalance() {
        return ResponseEntity.ok(productionService.getAmmoniaBalance());
    }

    @GetMapping("/usage")
    public ResponseEntity<List<AmmoniaUsageResponse>> getAmmoniaUsage(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(productionService.getAmmoniaUsage(days));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmmoniaRecordResponse> getAmmoniaRecord(@PathVariable String id) {
        return ResponseEntity.ok(productionService.getAmmoniaRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AmmoniaRecordResponse>> getAllAmmoniaRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productionService.getAllAmmoniaRecords(type, from, to, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AmmoniaRecordResponse> updateAmmoniaRecord(
            @PathVariable String id, @Valid @RequestBody AmmoniaRecordRequest request) {
        return ResponseEntity.ok(productionService.updateAmmoniaRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAmmoniaRecord(@PathVariable String id) {
        productionService.deleteAmmoniaRecord(id);
        return ResponseEntity.noContent().build();
    }
}
