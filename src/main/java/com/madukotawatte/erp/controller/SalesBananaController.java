package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesBananaRequest;
import com.madukotawatte.erp.dto.sales.SalesBananaResponse;
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
@RequestMapping("/api/v1/sales/banana")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesBananaController {

    private final FinanceService financeService;

    @PostMapping
    public ResponseEntity<SalesBananaResponse> createSalesBanana(@Valid @RequestBody SalesBananaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createSalesBanana(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesBananaResponse> getSalesBanana(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getSalesBanana(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesBananaResponse>> getAllSalesBanana(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllSalesBanana(pageable));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesBananaResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(financeService.markSalesBananaPaid(id, request));
    }
}
