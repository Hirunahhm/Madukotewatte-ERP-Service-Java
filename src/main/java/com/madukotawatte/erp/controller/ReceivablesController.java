package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.finance.ReceivablesResponse;
import com.madukotawatte.erp.service.ReceivablesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/receivables")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ReceivablesController {

    private final ReceivablesService receivablesService;

    @GetMapping
    public ResponseEntity<ReceivablesResponse> getReceivables(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(receivablesService.getReceivables(asOf));
    }
}
