package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.sales.SalesGeneralRequest;
import com.madukotawatte.erp.dto.sales.SalesGeneralResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.service.SalesService;
import com.madukotawatte.erp.service.support.ProductCatalog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sales/general")
@RequiredArgsConstructor
public class SalesGeneralController {

    private final SalesService salesService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalesGeneralResponse> create(@Valid @RequestBody SalesGeneralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.createSalesGeneral(request));
    }

    /** Allowed products and units for the form dropdowns. */
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<Map<String, List<String>>> options() {
        return ResponseEntity.ok(Map.of("products", ProductCatalog.PRODUCTS, "units", ProductCatalog.UNITS));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<SalesGeneralResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(salesService.getSalesGeneral(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<SalesGeneralResponse>> list(
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String buyer,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "saleDate") Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSalesGeneral(product, buyer, isPaid, from, to, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalesGeneralResponse> update(
            @PathVariable String id, @Valid @RequestBody SalesGeneralRequest request) {
        return ResponseEntity.ok(salesService.updateSalesGeneral(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        salesService.deleteSalesGeneral(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalesGeneralResponse> markPaid(
            @PathVariable String id, @Valid @RequestBody SalesMarkPaidRequest request) {
        return ResponseEntity.ok(salesService.markSalesGeneralPaid(id, request));
    }

    @PostMapping("/{id}/payment/undo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalesGeneralResponse> undoPayment(@PathVariable String id) {
        return ResponseEntity.ok(salesService.undoSalesGeneralPayment(id));
    }
}
