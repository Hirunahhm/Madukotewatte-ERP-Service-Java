package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexRequest;
import com.madukotawatte.erp.dto.sales.SalesLatexResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexUpdateRequest;
import com.madukotawatte.erp.service.FinanceService;
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
@RequestMapping("/api/v1/sales/latex")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesLatexController {

    private final FinanceService financeService;

    @PostMapping
    public ResponseEntity<SalesLatexResponse> createSalesLatex(@Valid @RequestBody SalesLatexRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createSalesLatex(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesLatexResponse> getSalesLatex(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getSalesLatex(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesLatexResponse>> getAllSalesLatex(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllSalesLatex(pageable));
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<SalesLatexResponse>> getUnpaidSales() {
        return ResponseEntity.ok(financeService.getUnpaidSales());
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesLatexResponse> markPaymentReceived(
            @PathVariable String id, @RequestBody SalesLatexUpdateRequest request) {
        return ResponseEntity.ok(financeService.markPaymentReceived(id, request));
    }
}
