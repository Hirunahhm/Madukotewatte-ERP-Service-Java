package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetSummaryResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetUpdateRequest;
import com.madukotawatte.erp.service.FixedAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fixed-assets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

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

    @PatchMapping("/{id}")
    public ResponseEntity<FixedAssetResponse> updateFixedAsset(
            @PathVariable String id, @Valid @RequestBody FixedAssetUpdateRequest request) {
        return ResponseEntity.ok(fixedAssetService.updateFixedAsset(id, request));
    }
}
