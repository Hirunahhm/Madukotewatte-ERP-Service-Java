package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.estateloan.BorrowingScheduleResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanDrawdownRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanMovementResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanPaymentRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanUpdateRequest;
import com.madukotawatte.erp.dto.estateloan.LoanBalanceAsOfResponse;
import com.madukotawatte.erp.service.EstateLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final EstateLoanService estateLoanService;

    @GetMapping("/loans")
    public ResponseEntity<List<EstateLoanResponse>> getEstateLoans() {
        return ResponseEntity.ok(estateLoanService.getEstateLoans());
    }

    @GetMapping("/loans/{id}")
    public ResponseEntity<EstateLoanResponse> getEstateLoan(@PathVariable String id) {
        return ResponseEntity.ok(estateLoanService.getEstateLoan(id));
    }

    @PostMapping("/loans")
    public ResponseEntity<EstateLoanResponse> createEstateLoan(@Valid @RequestBody EstateLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estateLoanService.createEstateLoan(request));
    }

    @PutMapping("/loans/{id}")
    public ResponseEntity<EstateLoanResponse> updateEstateLoan(
            @PathVariable String id, @Valid @RequestBody EstateLoanUpdateRequest request) {
        return ResponseEntity.ok(estateLoanService.updateEstateLoan(id, request));
    }

    /** Only while the loan has just its opening borrowing (entered in error). */
    @DeleteMapping("/loans/{id}")
    public ResponseEntity<Void> deleteEstateLoan(@PathVariable String id) {
        estateLoanService.deleteEstateLoan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/loans/{id}/movements")
    public ResponseEntity<List<EstateLoanMovementResponse>> getMovements(@PathVariable String id) {
        return ResponseEntity.ok(estateLoanService.getMovements(id));
    }

    @PostMapping("/loans/{id}/payments")
    public ResponseEntity<EstateLoanResponse> recordEstateLoanPayment(
            @PathVariable String id, @Valid @RequestBody EstateLoanPaymentRequest request) {
        return ResponseEntity.ok(estateLoanService.recordEstateLoanPayment(id, request));
    }

    @PostMapping("/loans/{id}/drawdowns")
    public ResponseEntity<EstateLoanResponse> recordDrawdown(
            @PathVariable String id, @Valid @RequestBody EstateLoanDrawdownRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estateLoanService.recordDrawdown(id, request));
    }

    /** Undo the latest repayment or drawdown of the loan. */
    @DeleteMapping("/loans/{id}/movements/latest")
    public ResponseEntity<EstateLoanResponse> undoLastMovement(@PathVariable String id) {
        return ResponseEntity.ok(estateLoanService.undoLastMovement(id));
    }

    @GetMapping("/schedule")
    public ResponseEntity<BorrowingScheduleResponse> getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(estateLoanService.schedule(from, to));
    }

    @GetMapping("/balances")
    public ResponseEntity<List<LoanBalanceAsOfResponse>> getBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(estateLoanService.balancesAsOf(asOf != null ? asOf : LocalDate.now()));
    }
}
