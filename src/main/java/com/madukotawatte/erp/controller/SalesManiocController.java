package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesManiocRequest;
import com.madukotawatte.erp.dto.sales.SalesManiocResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/manioc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesManiocController {

    private final FinanceService financeService;

    @PostMapping
    public ResponseEntity<SalesManiocResponse> createSalesManioc(@Valid @RequestBody SalesManiocRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createSalesManioc(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesManiocResponse> getSalesManioc(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getSalesManioc(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesManiocResponse>> getAllSalesManioc(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllSalesManioc(pageable));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesManiocResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(financeService.markSalesManiocPaid(id, request));
    }
}
