package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesManiocRequest;
import com.madukotawatte.erp.dto.sales.SalesManiocResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/manioc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesManiocController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<SalesManiocResponse> createSalesManioc(@Valid @RequestBody SalesManiocRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.createSalesManioc(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesManiocResponse> getSalesManioc(@PathVariable String id) {
        return ResponseEntity.ok(salesService.getSalesManioc(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesManiocResponse>> getAllSalesManioc(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSalesManioc(pageable));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesManiocResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(salesService.markSalesManiocPaid(id, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesManiocResponse> updateSalesManioc(
            @PathVariable String id, @Valid @RequestBody SalesManiocRequest request) {
        return ResponseEntity.ok(salesService.updateSalesManioc(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalesManioc(@PathVariable String id) {
        salesService.deleteSalesManioc(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payment/undo")
    public ResponseEntity<SalesManiocResponse> undoPayment(@PathVariable String id) {
        return ResponseEntity.ok(salesService.undoSalesManiocPayment(id));
    }
}
