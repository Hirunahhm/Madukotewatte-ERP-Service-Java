package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordRequest;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
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
@RequestMapping("/api/v1/rubber-solid-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class RubberSolidRecordController {

    private final DailyOperationsService dailyOperationsService;

    @PostMapping
    public ResponseEntity<RubberSolidRecordResponse> createRubberSolidRecord(@Valid @RequestBody RubberSolidRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dailyOperationsService.createRubberSolidRecord(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RubberSolidRecordResponse>> getAllRubberSolidRecords(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dailyOperationsService.getAllRubberSolidRecords(pageable));
    }

    @GetMapping("/load/{loadId}")
    public ResponseEntity<List<RubberSolidRecordResponse>> getRubberSolidRecordsByLoad(@PathVariable String loadId) {
        return ResponseEntity.ok(dailyOperationsService.getRubberSolidRecordsByLoad(loadId));
    }
}
