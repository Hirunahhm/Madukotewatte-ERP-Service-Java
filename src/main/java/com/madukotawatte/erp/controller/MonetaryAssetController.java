package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionRequest;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionResponse;
import com.madukotawatte.erp.service.FinanceService;
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

@RestController
@RequestMapping("/api/v1/monetary-assets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MonetaryAssetController {

    private final FinanceService financeService;

    @GetMapping("/balances")
    public ResponseEntity<List<AssetBalanceResponse>> getAssetBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(financeService.getAssetBalances(asOf));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<com.madukotawatte.erp.dto.finance.TrendPointResponse>> getAssetBalanceTrend(
            @RequestParam(defaultValue = "month") String scale) {
        return ResponseEntity.ok(financeService.getAssetBalanceTrend(scale));
    }

    @PostMapping("/transactions")
    public ResponseEntity<MonetaryAssetTransactionResponse> createTransaction(
            @Valid @RequestBody MonetaryAssetTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createMonetaryTransaction(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<MonetaryAssetTransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllMonetaryTransactions(assetType, transactionType, from, to, pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<MonetaryAssetTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getMonetaryTransaction(id));
    }
}
