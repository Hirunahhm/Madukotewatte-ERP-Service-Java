package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.service.WorkforceService;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final WorkforceService workforceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workforceService.getAllEmployees(name, pageable));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<EmployeeSummaryResponse>> getAllEmployeesSummary() {
        return ResponseEntity.ok(workforceService.getAllEmployeesSummary());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable String id) {
        return ResponseEntity.ok(workforceService.getEmployee(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workforceService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable String id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(workforceService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        workforceService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<AttendanceResponse>> getEmployeeAttendance(
            @PathVariable String id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workforceService.getEmployeeAttendance(id, pageable));
    }

    @GetMapping("/{id}/attendance/range")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AttendanceResponse>> getEmployeeAttendanceByRange(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(workforceService.getEmployeeAttendanceByDateRange(id, from, to));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<EmployeeTransactionResponse>> getEmployeeTransactions(@PathVariable String id) {
        return ResponseEntity.ok(workforceService.getEmployeeTransactions(id));
    }
}
