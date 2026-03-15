package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
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
@RequestMapping("/api/v1/estate-loans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EstateLoanController {

    private final FinanceService financeService;

    @GetMapping("/balances")
    public ResponseEntity<List<EstateLoanBalanceResponse>> getEstateLoanBalances() {
        return ResponseEntity.ok(financeService.getEstateLoanBalances());
    }

    @PostMapping("/transactions")
    public ResponseEntity<EstateLoanTransactionResponse> createTransaction(
            @Valid @RequestBody EstateLoanTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createEstateLoanTransaction(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<EstateLoanTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllEstateLoanTransactions(pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<EstateLoanTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getEstateLoanTransaction(id));
    }
}
