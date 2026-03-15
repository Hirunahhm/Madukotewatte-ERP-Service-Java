package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.UpdateLoanRequest;
import com.madukotawatte.erp.service.WorkforceService;
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

    private final WorkforceService workforceService;

    @PostMapping
    public ResponseEntity<EmployeeLoanResponse> createLoan(@Valid @RequestBody EmployeeLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workforceService.createLoan(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeLoanResponse> getLoan(@PathVariable String id) {
        return ResponseEntity.ok(workforceService.getLoan(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeLoanResponse>> getEmployeeLoans(@PathVariable String employeeId) {
        return ResponseEntity.ok(workforceService.getEmployeeLoans(employeeId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmployeeLoanResponse>> getActiveLoans() {
        return ResponseEntity.ok(workforceService.getActiveLoans());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeLoanResponse> updateLoan(
            @PathVariable String id, @RequestBody UpdateLoanRequest request) {
        return ResponseEntity.ok(workforceService.updateLoan(id, request));
    }
}
