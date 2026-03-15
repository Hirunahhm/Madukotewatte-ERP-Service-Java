package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.latex.LatexRecordRequest;
import com.madukotawatte.erp.dto.latex.LatexRecordResponse;
import com.madukotawatte.erp.service.DailyOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/latex-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class LatexRecordController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    public ResponseEntity<LatexRecordResponse> createLatexRecord(@Valid @RequestBody LatexRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createLatexRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LatexRecordResponse> getLatexRecord(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getLatexRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<LatexRecordResponse>> getAllLatexRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllLatexRecords(pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<LatexRecordResponse>> getLatexRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(dailyOperationsService.getLatexRecordsByLoad(loadId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LatexRecordResponse> updateLatexRecord(
            @PathVariable String id, @Valid @RequestBody LatexRecordRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateLatexRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLatexRecord(@PathVariable String id) {
        dailyOperationsService.deleteLatexRecord(id);
        return ResponseEntity.noContent().build();
    }
}
