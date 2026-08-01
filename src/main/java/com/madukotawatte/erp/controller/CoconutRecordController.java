package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.coconut.CoconutLoadSummaryResponse;
import com.madukotawatte.erp.dto.coconut.CoconutRecordRequest;
import com.madukotawatte.erp.dto.coconut.CoconutRecordResponse;
import com.madukotawatte.erp.dto.coconut.CoconutVarietyBreakdownResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.service.CoconutProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coconut-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class CoconutRecordController {

    private final CoconutProductionService coconutProductionService;

    @PostMapping
    public ResponseEntity<CoconutRecordResponse> createCoconutRecord(@Valid @RequestBody CoconutRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coconutProductionService.createCoconutRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoconutRecordResponse> getCoconutRecord(@PathVariable String id) {
        return ResponseEntity.ok(coconutProductionService.getCoconutRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CoconutRecordResponse>> getAllCoconutRecords(
            @RequestParam(required = false) String variety,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(coconutProductionService.getAllCoconutRecords(variety, from, to, pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<CoconutRecordResponse>> getCoconutRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(coconutProductionService.getCoconutRecordsByLoad(loadId));
    }

    @GetMapping("/load/{loadId}/summary")
    public ResponseEntity<CoconutLoadSummaryResponse> getCoconutLoadSummary(@PathVariable String loadId) {
        return ResponseEntity.ok(coconutProductionService.getCoconutLoadSummary(loadId));
    }

    @GetMapping("/load/{loadId}/trends")
    public ResponseEntity<List<VolumeTrendResponse>> getCoconutLoadTrends(
            @PathVariable String loadId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(coconutProductionService.getCoconutLoadTrends(loadId, days));
    }

    @GetMapping("/variety-breakdown")
    public ResponseEntity<List<CoconutVarietyBreakdownResponse>> getVarietyBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(coconutProductionService.getVarietyBreakdown(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoconutRecordResponse> updateCoconutRecord(
            @PathVariable String id, @Valid @RequestBody CoconutRecordRequest request) {
        return ResponseEntity.ok(coconutProductionService.updateCoconutRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoconutRecord(@PathVariable String id) {
        coconutProductionService.deleteCoconutRecord(id);
        return ResponseEntity.noContent().build();
    }
}
