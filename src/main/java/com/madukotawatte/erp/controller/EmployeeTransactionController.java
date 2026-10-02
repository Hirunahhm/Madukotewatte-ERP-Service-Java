package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.AdvanceRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.service.EmployeeFinanceService;
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

    private final EmployeeFinanceService employeeFinanceService;

    /** Gives an advance: posts EMPLOYEE_ADVANCE from the chosen cash/bank account. */
    @PostMapping("/advances")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeTransactionResponse> giveAdvance(@Valid @RequestBody AdvanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeFinanceService.giveAdvance(request));
    }

    /** Undoes an advance that has not been recovered (reverses the ledger row). */
    @DeleteMapping("/advances/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdvance(@PathVariable String id) {
        employeeFinanceService.deleteAdvance(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeTransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(employeeFinanceService.getEmployeeTransaction(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeTransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeFinanceService.getAllEmployeeTransactions(pageable));
    }
}
