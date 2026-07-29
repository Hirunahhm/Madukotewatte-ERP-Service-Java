package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.expense.ExpenseMarkPaidRequest;
import com.madukotawatte.erp.dto.expense.ExpenseRequest;
import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.ExpenseSummaryResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExpenseController {

    private final FinanceService financeService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createExpense(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getExpense(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ExpenseResponse>> getAllExpenses(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(financeService.getAllExpenses(type, isPaid, from, to, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable String id, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(financeService.updateExpense(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable String id) {
        financeService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<ExpenseResponse> markExpensePaid(
            @PathVariable String id, @Valid @RequestBody ExpenseMarkPaidRequest request) {
        return ResponseEntity.ok(financeService.markExpensePaid(id, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryResponse> getExpenseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(financeService.getExpenseSummary(from, to));
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<CategoryTotalResponse>> getExpenseDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        return ResponseEntity.ok(financeService.getExpenseDistribution(from, to));
    }

    @GetMapping("/trend")
    public ResponseEntity<List<TrendPointResponse>> getExpenseTrend(
            @RequestParam(defaultValue = "month") String scale) {
        return ResponseEntity.ok(financeService.getExpenseTrend(scale));
    }
}
