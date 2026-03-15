package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.dashboard.DailyReportResponse;
import com.madukotawatte.erp.dto.dashboard.DashboardSummaryResponse;
import com.madukotawatte.erp.dto.dashboard.MonthlyPayrollResponse;
import com.madukotawatte.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/daily-report")
    public ResponseEntity<DailyReportResponse> getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dashboardService.getDailyReport(date));
    }

    @GetMapping("/payroll")
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
