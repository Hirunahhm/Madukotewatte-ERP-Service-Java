package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.UpdateLoanRequest;
import com.madukotawatte.erp.dto.loan.LoanRepaymentRequest;
import com.madukotawatte.erp.service.EmployeeFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-loans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeLoanController {

    private final EmployeeFinanceService financeService;

    @PostMapping
    public ResponseEntity<EmployeeLoanResponse> createLoan(@Valid @RequestBody EmployeeLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createLoan(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeLoanResponse> getLoan(@PathVariable String id) {
        return ResponseEntity.ok(financeService.getLoan(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeLoanResponse>> getEmployeeLoans(@PathVariable String employeeId) {
        return ResponseEntity.ok(financeService.getEmployeeLoans(employeeId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmployeeLoanResponse>> getActiveLoans() {
        return ResponseEntity.ok(financeService.getActiveLoans());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeLoanResponse> updateLoan(
            @PathVariable String id, @RequestBody UpdateLoanRequest request) {
        return ResponseEntity.ok(financeService.updateLoan(id, request));
    }

    /** The employee repays part of the loan in cash/bank. */
    @PostMapping("/{id}/repayments")
    public ResponseEntity<EmployeeLoanResponse> repayLoan(
            @PathVariable String id, @Valid @RequestBody LoanRepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.repayLoan(id, request));
    }

    @DeleteMapping("/{id}/repayments/{repaymentId}")
    public ResponseEntity<EmployeeLoanResponse> deleteRepayment(
            @PathVariable String id, @PathVariable String repaymentId) {
        return ResponseEntity.ok(financeService.deleteRepayment(id, repaymentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable String id) {
        financeService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }
}
