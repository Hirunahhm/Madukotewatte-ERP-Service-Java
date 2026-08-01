package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.dto.manioc.ManiocLoadSummaryResponse;
import com.madukotawatte.erp.dto.manioc.ManiocRecordRequest;
import com.madukotawatte.erp.dto.manioc.ManiocRecordResponse;
import com.madukotawatte.erp.dto.manioc.ManiocVarietyBreakdownResponse;
import com.madukotawatte.erp.service.ManiocProductionService;
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
@RequestMapping("/api/v1/manioc-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ManiocRecordController {

    private final ManiocProductionService maniocProductionService;

    @PostMapping
    public ResponseEntity<ManiocRecordResponse> createManiocRecord(@Valid @RequestBody ManiocRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maniocProductionService.createManiocRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManiocRecordResponse> getManiocRecord(@PathVariable String id) {
        return ResponseEntity.ok(maniocProductionService.getManiocRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ManiocRecordResponse>> getAllManiocRecords(
            @RequestParam(required = false) String variety,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(maniocProductionService.getAllManiocRecords(variety, from, to, pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<ManiocRecordResponse>> getManiocRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(maniocProductionService.getManiocRecordsByLoad(loadId));
    }

    @GetMapping("/load/{loadId}/summary")
    public ResponseEntity<ManiocLoadSummaryResponse> getManiocLoadSummary(@PathVariable String loadId) {
        return ResponseEntity.ok(maniocProductionService.getManiocLoadSummary(loadId));
    }

    @GetMapping("/load/{loadId}/trends")
    public ResponseEntity<List<VolumeTrendResponse>> getManiocLoadTrends(
            @PathVariable String loadId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(maniocProductionService.getManiocLoadTrends(loadId, days));
    }

    @GetMapping("/variety-breakdown")
    public ResponseEntity<List<ManiocVarietyBreakdownResponse>> getVarietyBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(maniocProductionService.getVarietyBreakdown(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManiocRecordResponse> updateManiocRecord(
            @PathVariable String id, @Valid @RequestBody ManiocRecordRequest request) {
        return ResponseEntity.ok(maniocProductionService.updateManiocRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManiocRecord(@PathVariable String id) {
        maniocProductionService.deleteManiocRecord(id);
        return ResponseEntity.noContent().build();
    }
}
