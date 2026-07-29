package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.dto.sales.SalesLedgerRowResponse;
import com.madukotawatte.erp.dto.sales.SalesSummaryResponse;
import com.madukotawatte.erp.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinancialsController {

    private final FinanceService financeService;

    @GetMapping("/ledger")
    public ResponseEntity<PageResponse<SalesLedgerRowResponse>> getSalesLedger(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getSalesLedger(category, status, from, to, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(financeService.getSalesSummary(from, to));
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<CategoryTotalResponse>> getSalesDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(financeService.getSalesDistribution(from, to));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendPointResponse>> getSalesTrend(
            @RequestParam(defaultValue = "month") String scale) {
        return ResponseEntity.ok(financeService.getSalesTrend(scale));
    }
}
