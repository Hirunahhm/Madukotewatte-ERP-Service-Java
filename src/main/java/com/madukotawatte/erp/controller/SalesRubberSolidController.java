package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidResponse;
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
@RequestMapping("/api/v1/sales/rubber-solid")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesRubberSolidController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<SalesRubberSolidResponse> createSalesRubberSolid(@Valid @RequestBody SalesRubberSolidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.createSalesRubberSolid(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesRubberSolidResponse> getSalesRubberSolid(@PathVariable String id) {
        return ResponseEntity.ok(salesService.getSalesRubberSolid(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SalesRubberSolidResponse>> getAllSalesRubberSolid(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSalesRubberSolid(pageable));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesRubberSolidResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(salesService.markSalesRubberSolidPaid(id, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesRubberSolidResponse> updateSalesRubberSolid(
            @PathVariable String id, @Valid @RequestBody SalesRubberSolidRequest request) {
        return ResponseEntity.ok(salesService.updateSalesRubberSolid(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalesRubberSolid(@PathVariable String id) {
        salesService.deleteSalesRubberSolid(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/payment/undo")
    public ResponseEntity<SalesRubberSolidResponse> undoPayment(@PathVariable String id) {
        return ResponseEntity.ok(salesService.undoSalesRubberSolidPayment(id));
    }
}
