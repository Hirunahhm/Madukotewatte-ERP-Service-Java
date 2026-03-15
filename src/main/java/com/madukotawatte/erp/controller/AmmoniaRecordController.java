package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordRequest;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.service.DailyOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ammonia-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AmmoniaRecordController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    public ResponseEntity<AmmoniaRecordResponse> createAmmoniaRecord(@Valid @RequestBody AmmoniaRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createAmmoniaRecord(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AmmoniaRecordResponse>> getAllAmmoniaRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllAmmoniaRecords(pageable));
    }
}
