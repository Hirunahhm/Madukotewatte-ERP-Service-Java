package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.payroll.PayrollPreviewRow;
import com.madukotawatte.erp.dto.payroll.PayrollSummaryResponse;
import com.madukotawatte.erp.dto.payroll.PayslipRequest;
import com.madukotawatte.erp.dto.payroll.PayslipResponse;
import com.madukotawatte.erp.dto.payroll.PayslipVoidRequest;
import com.madukotawatte.erp.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<PayrollPreviewRow>> preview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String employeeId) {
        return ResponseEntity.ok(payrollService.preview(from, to, employeeId));
    }

    @PostMapping("/payslips")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayslipResponse> createPayslip(@Valid @RequestBody PayslipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayslip(request));
    }

    /** All-or-nothing: the body is a JSON array of payslip requests. */
    @PostMapping("/payslips/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PayslipResponse>> createBulk(@RequestBody List<@Valid PayslipRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createBulk(requests));
    }

    @GetMapping("/payslips")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<PayslipResponse>> listPayslips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(payrollService.listPayslips(from, to, employeeId, pageable));
    }

    @GetMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PayslipResponse> getPayslip(@PathVariable String id) {
        return ResponseEntity.ok(payrollService.getPayslip(id));
    }

    @PostMapping("/payslips/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayslipResponse> voidPayslip(
            @PathVariable String id, @RequestBody(required = false) PayslipVoidRequest request) {
        return ResponseEntity.ok(payrollService.voidPayslip(id, request != null ? request.getReason() : null));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PayrollSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(payrollService.summary(from, to));
    }
}
