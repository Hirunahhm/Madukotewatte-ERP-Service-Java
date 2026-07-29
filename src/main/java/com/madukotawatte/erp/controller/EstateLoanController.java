package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
import com.madukotawatte.erp.dto.estateloan.CreditCardLimitResponse;
import com.madukotawatte.erp.dto.estateloan.CreditCardStatementResponse;
import com.madukotawatte.erp.dto.estateloan.UpdateCreditCardLimitRequest;
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
@RequestMapping("/api/v1/estate-loans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EstateLoanController {

    private final FinanceService financeService;

    @GetMapping("/balances")
    public ResponseEntity<List<EstateLoanBalanceResponse>> getEstateLoanBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(financeService.getEstateLoanBalances(asOf));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<com.madukotawatte.erp.dto.finance.TrendPointResponse>> getLoanBalanceTrend(
            @RequestParam(defaultValue = "month") String scale) {
        return ResponseEntity.ok(financeService.getLoanBalanceTrend(scale));
    }

    @PostMapping("/transactions")
    public ResponseEntity<EstateLoanTransactionResponse> createTransaction(
            @Valid @RequestBody EstateLoanTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createEstateLoanTransaction(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<EstateLoanTransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String loanType,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllEstateLoanTransactions(loanType, transactionType, from, to, pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<EstateLoanTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getEstateLoanTransaction(id));
    }

    @GetMapping("/credit-cards/statement")
    public ResponseEntity<CreditCardStatementResponse> getCreditCardStatement(
            @RequestParam String loanType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(financeService.getCreditCardStatement(loanType, from, to));
    }

    @GetMapping("/credit-cards/limits")
    public ResponseEntity<List<CreditCardLimitResponse>> getCreditCardLimits() {
        return ResponseEntity.ok(financeService.getCreditCardLimits());
    }

    @PutMapping("/credit-cards/limit")
    public ResponseEntity<CreditCardLimitResponse> updateCreditCardLimit(
            @Valid @RequestBody UpdateCreditCardLimitRequest request) {
        return ResponseEntity.ok(financeService.updateCreditCardLimit(request));
    }
}
