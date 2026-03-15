package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionRequest;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionResponse;
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
@RequestMapping("/api/v1/monetary-assets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MonetaryAssetController {

    private final FinanceService financeService;

    @GetMapping("/balances")
    public ResponseEntity<List<AssetBalanceResponse>> getAssetBalances() {
        return ResponseEntity.ok(financeService.getAssetBalances());
    }

    @PostMapping("/transactions")
    public ResponseEntity<MonetaryAssetTransactionResponse> createTransaction(
            @Valid @RequestBody MonetaryAssetTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createMonetaryTransaction(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<MonetaryAssetTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllMonetaryTransactions(pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<MonetaryAssetTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getMonetaryTransaction(id));
    }
}
