package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesCoconutRequest;
import com.madukotawatte.erp.dto.sales.SalesCoconutResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/coconut")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesCoconutController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<SalesCoconutResponse> createSalesCoconut(@Valid @RequestBody SalesCoconutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.createSalesCoconut(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesCoconutResponse> getSalesCoconut(@PathVariable String id) {
        return ResponseEntity.ok(salesService.getSalesCoconut(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesCoconutResponse>> getAllSalesCoconut(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSalesCoconut(pageable));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesCoconutResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(salesService.markSalesCoconutPaid(id, request));
    }
}
