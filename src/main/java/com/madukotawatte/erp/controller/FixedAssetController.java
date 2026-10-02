package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.fixedasset.DepreciationScheduleResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetDisposalRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetSummaryResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetUpdateRequest;
import com.madukotawatte.erp.service.DepreciationService;
import com.madukotawatte.erp.service.FixedAssetService;
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
@RequestMapping("/api/v1/fixed-assets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;
    private final DepreciationService depreciationService;

    @PostMapping
    public ResponseEntity<FixedAssetResponse> createFixedAsset(@Valid @RequestBody FixedAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fixedAssetService.createFixedAsset(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<FixedAssetResponse>> getAllFixedAssets(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(fixedAssetService.getAllFixedAssets(category, status, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<FixedAssetSummaryResponse> getFixedAssetSummary() {
        return ResponseEntity.ok(fixedAssetService.getFixedAssetSummary());
    }

    /** Fixed-asset / depreciation schedule for the period (cost, accumulated depreciation, NBV, disposals). */
    @GetMapping("/depreciation")
    public ResponseEntity<DepreciationScheduleResponse> getDepreciationSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(depreciationService.schedule(from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedAssetResponse> getFixedAsset(@PathVariable String id) {
        return ResponseEntity.ok(fixedAssetService.getFixedAsset(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FixedAssetResponse> updateFixedAsset(
            @PathVariable String id, @Valid @RequestBody FixedAssetUpdateRequest request) {
        return ResponseEntity.ok(fixedAssetService.updateFixedAsset(id, request));
    }

    @PostMapping("/{id}/dispose")
    public ResponseEntity<FixedAssetResponse> disposeFixedAsset(
            @PathVariable String id, @Valid @RequestBody FixedAssetDisposalRequest request) {
        return ResponseEntity.ok(fixedAssetService.disposeFixedAsset(id, request));
    }

    /** Only for assets created in error (not disposed); reverses the purchase ledger row. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFixedAsset(@PathVariable String id) {
        fixedAssetService.deleteFixedAsset(id);
        return ResponseEntity.noContent().build();
    }
}
