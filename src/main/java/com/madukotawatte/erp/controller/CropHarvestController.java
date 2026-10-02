package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestRequest;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestResponse;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestSummaryRow;
import com.madukotawatte.erp.service.CropHarvestService;
import com.madukotawatte.erp.service.support.ProductCatalog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/crop-harvests")
@RequiredArgsConstructor
public class CropHarvestController {

    private final CropHarvestService cropHarvestService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CropHarvestResponse> create(@Valid @RequestBody CropHarvestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropHarvestService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<CropHarvestResponse>> list(
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String fieldBlock,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "harvestDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cropHarvestService.getAll(crop, fieldBlock, from, to, pageable));
    }

    /** Quantity per crop per month (and unit). */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<CropHarvestSummaryRow>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(cropHarvestService.summary(from, to));
    }

    /** Allowed crops and units for the form dropdowns. */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, List<String>>> options() {
        return ResponseEntity.ok(Map.of("crops", ProductCatalog.CROPS, "units", ProductCatalog.UNITS));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<CropHarvestResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(cropHarvestService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CropHarvestResponse> update(@PathVariable String id, @Valid @RequestBody CropHarvestRequest request) {
        return ResponseEntity.ok(cropHarvestService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        cropHarvestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
