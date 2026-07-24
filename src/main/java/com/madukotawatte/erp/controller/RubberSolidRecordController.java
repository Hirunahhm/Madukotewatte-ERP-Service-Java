package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberLoadSummaryResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordRequest;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
import com.madukotawatte.erp.service.DailyOperationsService;
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
@RequestMapping("/api/v1/rubber-solid-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class RubberSolidRecordController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    public ResponseEntity<RubberSolidRecordResponse> createRubberSolidRecord(@Valid @RequestBody RubberSolidRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createRubberSolidRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RubberSolidRecordResponse> getRubberSolidRecord(@PathVariable String id) {
        return ResponseEntity.ok(dailyOperationsService.getRubberSolidRecord(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RubberSolidRecordResponse>> getAllRubberSolidRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllRubberSolidRecords(from, to, pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<RubberSolidRecordResponse>> getRubberSolidRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(dailyOperationsService.getRubberSolidRecordsByLoad(loadId));
    }

    @GetMapping("/load/{loadId}/summary")
    public ResponseEntity<RubberLoadSummaryResponse> getRubberLoadSummary(@PathVariable String loadId) {
        return ResponseEntity.ok(dailyOperationsService.getRubberLoadSummary(loadId));
    }

    @GetMapping("/load/{loadId}/trends")
    public ResponseEntity<List<VolumeTrendResponse>> getRubberLoadTrends(
            @PathVariable String loadId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dailyOperationsService.getRubberLoadTrends(loadId, days));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RubberSolidRecordResponse> updateRubberSolidRecord(
            @PathVariable String id, @Valid @RequestBody RubberSolidRecordRequest request) {
        return ResponseEntity.ok(dailyOperationsService.updateRubberSolidRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRubberSolidRecord(@PathVariable String id) {
        dailyOperationsService.deleteRubberSolidRecord(id);
        return ResponseEntity.noContent().build();
    }
}
