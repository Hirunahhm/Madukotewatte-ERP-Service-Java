package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.banana.BananaLoadSummaryResponse;
import com.madukotawatte.erp.dto.banana.BananaRecordRequest;
import com.madukotawatte.erp.dto.banana.BananaRecordResponse;
import com.madukotawatte.erp.dto.banana.BananaVarietyBreakdownResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.service.BananaProductionService;
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
@RequestMapping("/api/v1/banana-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class BananaRecordController {

    private final BananaProductionService bananaProductionService;

    @PostMapping
    public ResponseEntity<BananaRecordResponse> createBananaRecord(@Valid @RequestBody BananaRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bananaProductionService.createBananaRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BananaRecordResponse> getBananaRecord(@PathVariable String id) {
        return ResponseEntity.ok(bananaProductionService.getBananaRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BananaRecordResponse>> getAllBananaRecords(
            @RequestParam(required = false) String variety,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bananaProductionService.getAllBananaRecords(variety, from, to, pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<BananaRecordResponse>> getBananaRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(bananaProductionService.getBananaRecordsByLoad(loadId));
    }

    @GetMapping("/load/{loadId}/summary")
    public ResponseEntity<BananaLoadSummaryResponse> getBananaLoadSummary(@PathVariable String loadId) {
        return ResponseEntity.ok(bananaProductionService.getBananaLoadSummary(loadId));
    }

    @GetMapping("/load/{loadId}/trends")
    public ResponseEntity<List<VolumeTrendResponse>> getBananaLoadTrends(
            @PathVariable String loadId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(bananaProductionService.getBananaLoadTrends(loadId, days));
    }

    @GetMapping("/variety-breakdown")
    public ResponseEntity<List<BananaVarietyBreakdownResponse>> getVarietyBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(bananaProductionService.getVarietyBreakdown(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BananaRecordResponse> updateBananaRecord(
            @PathVariable String id, @Valid @RequestBody BananaRecordRequest request) {
        return ResponseEntity.ok(bananaProductionService.updateBananaRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBananaRecord(@PathVariable String id) {
        bananaProductionService.deleteBananaRecord(id);
        return ResponseEntity.noContent().build();
    }
}
