package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.dashboard.DashboardOverviewResponse;
import com.madukotawatte.erp.dto.dashboard.MonthlyPayrollResponse;
import com.madukotawatte.erp.service.DashboardOverviewService;
import com.madukotawatte.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardOverviewService dashboardOverviewService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<DashboardOverviewResponse> getOverview(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(dashboardOverviewService.overview(month));
    }

    @GetMapping("/payroll")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<MonthlyPayrollResponse> getMonthlyPayroll(
            @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(dashboardService.getMonthlyPayroll(month, year));
    }

    @GetMapping("/export/payroll")
    public ResponseEntity<byte[]> exportPayroll(
            @RequestParam int month, @RequestParam int year) {
        byte[] data = dashboardService.exportPayroll(month, year);
        String filename = "payroll-" + year + "-" + month + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
