package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
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
@RequestMapping("/api/v1/employee-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class EmployeeTransactionController {

    private final FinanceService financeService;

    @PostMapping
    public ResponseEntity<EmployeeTransactionResponse> createTransaction(
            @Valid @RequestBody EmployeeTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createEmployeeTransaction(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getEmployeeTransaction(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllEmployeeTransactions(pageable));
    }
}
