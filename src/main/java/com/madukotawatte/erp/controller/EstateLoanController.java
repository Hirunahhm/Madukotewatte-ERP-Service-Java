package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.estateloan.EstateLoanPaymentRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.service.EstateLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final EstateLoanService estateLoanService;

    @GetMapping("/loans")
    public ResponseEntity<List<EstateLoanResponse>> getEstateLoans() {
        return ResponseEntity.ok(estateLoanService.getEstateLoans());
    }

    @PostMapping("/loans")
    public ResponseEntity<EstateLoanResponse> createEstateLoan(@Valid @RequestBody EstateLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estateLoanService.createEstateLoan(request));
    }

    @PostMapping("/loans/{id}/payments")
    public ResponseEntity<EstateLoanResponse> recordEstateLoanPayment(
            @PathVariable String id, @Valid @RequestBody EstateLoanPaymentRequest request) {
        return ResponseEntity.ok(estateLoanService.recordEstateLoanPayment(id, request));
    }
}
