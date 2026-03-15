package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.UpdateLoanRequest;
import com.madukotawatte.erp.entity.Attendance;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.AttendanceMapper;
import com.madukotawatte.erp.mapper.EmployeeLoanMapper;
import com.madukotawatte.erp.mapper.EmployeeMapper;
import com.madukotawatte.erp.mapper.EmployeeTransactionMapper;
import com.madukotawatte.erp.repository.AttendanceRepository;
import com.madukotawatte.erp.repository.EmployeeLoanRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkforceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final AttendanceRepository attendanceRepository;

    // Employee CRUD
    public PageResponse<EmployeeResponse> getAllEmployees(String name, Pageable pageable) {
        Page<Employee> page;
        if (name != null && !name.isBlank()) {
            page = employeeRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            page = employeeRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(EmployeeMapper::toResponse));
    }

    public EmployeeResponse getEmployee(String id) {
        Employee employee = findEmployeeById(id);
        return EmployeeMapper.toResponse(employee);
    }

    public List<EmployeeSummaryResponse> getAllEmployeesSummary() {
        return employeeRepository.findAll().stream()
                .map(EmployeeMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Employee employee = EmployeeMapper.toEntity(request);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(String id, EmployeeRequest request) {
        Employee employee = findEmployeeById(id);
        employee.setName(request.getName());
        employee.setJoinedDate(request.getJoinedDate());
        employee.setSalary(request.getSalary());
        employee.setPosition(request.getPosition());
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(String id) {
        Employee employee = findEmployeeById(id);
        List<EmployeeLoan> activeLoans = employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(id);
        if (!activeLoans.isEmpty()) {
            throw new BadRequestException("Cannot delete employee with active loans");
        }
        employeeRepository.delete(employee);
    }

    // Loan operations
    @Transactional
    public EmployeeLoanResponse createLoan(EmployeeLoanRequest request) {
        Employee employee = findEmployeeById(request.getEmployeeId());
        List<EmployeeLoan> activeLoans = employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(request.getEmployeeId());
        if (!activeLoans.isEmpty()) {
            throw new BadRequestException("Employee already has an active loan");
        }
        EmployeeLoan loan = EmployeeLoanMapper.toEntity(request, employee);
        return EmployeeLoanMapper.toResponse(employeeLoanRepository.save(loan));
    }

    public List<EmployeeLoanResponse> getEmployeeLoans(String employeeId) {
        findEmployeeById(employeeId);
        return employeeLoanRepository.findByEmployee_EmployeeId(employeeId).stream()
                .map(EmployeeLoanMapper::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeLoanResponse getLoan(String loanId) {
        EmployeeLoan loan = employeeLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeLoan", "id", loanId));
        return EmployeeLoanMapper.toResponse(loan);
    }

    @Transactional
    public EmployeeLoanResponse updateLoan(String loanId, UpdateLoanRequest request) {
        EmployeeLoan loan = employeeLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeLoan", "id", loanId));
        if (request.getIsActive() != null) {
            loan.setIsActive(request.getIsActive());
        }
        if (request.getCurrentBalance() != null) {
            loan.setCurrentBalance(request.getCurrentBalance());
        }
        if (request.getInstallment() != null) {
            loan.setInstallment(request.getInstallment());
        }
        return EmployeeLoanMapper.toResponse(employeeLoanRepository.save(loan));
    }

    public List<EmployeeLoanResponse> getActiveLoans() {
        return employeeLoanRepository.findByIsActiveTrue().stream()
                .map(EmployeeLoanMapper::toResponse)
                .collect(Collectors.toList());
    }

    // Employee transactions
    public List<EmployeeTransactionResponse> getEmployeeTransactions(String employeeId) {
        findEmployeeById(employeeId);
        return employeeTransactionRepository.findByEmployee_EmployeeId(employeeId).stream()
                .map(EmployeeTransactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<EmployeeTransactionResponse> getEmployeeTransactionsPaged(
            String employeeId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        findEmployeeById(employeeId);
        Page<EmployeeTransactionResponse> page = employeeTransactionRepository
                .findByEmployee_EmployeeIdAndTimestampBetween(employeeId, from, to, pageable)
                .map(EmployeeTransactionMapper::toResponse);
        return PageResponse.from(page);
    }

    // Employee attendance
    public PageResponse<AttendanceResponse> getEmployeeAttendance(String employeeId, Pageable pageable) {
        findEmployeeById(employeeId);
        Page<AttendanceResponse> page = attendanceRepository
                .findByEmployee_EmployeeId(employeeId, pageable)
                .map(AttendanceMapper::toResponse);
        return PageResponse.from(page);
    }

    public List<AttendanceResponse> getEmployeeAttendanceByDateRange(
            String employeeId, LocalDateTime from, LocalDateTime to) {
        findEmployeeById(employeeId);
        return attendanceRepository.findByEmployee_EmployeeIdAndTimestampBetween(employeeId, from, to)
                .stream().map(AttendanceMapper::toResponse).collect(Collectors.toList());
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
