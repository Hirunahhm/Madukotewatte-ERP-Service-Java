package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeAttendanceStatsResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.dto.employee.PaymentSummaryResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionStatsResponse;
import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.UpdateLoanRequest;
import com.madukotawatte.erp.entity.Attendance;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.entity.EmployeeTransaction;
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
import com.madukotawatte.erp.repository.LabourRepository;
import com.madukotawatte.erp.entity.Labour;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final LabourRepository labourRepository;

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
        if (request.getRatePerTree() != null) {
            employee.setRatePerTree(request.getRatePerTree());
        }
        if (request.getRatePerBunch() != null) {
            employee.setRatePerBunch(request.getRatePerBunch());
        }
        if (request.getRatePerNut() != null) {
            employee.setRatePerNut(request.getRatePerNut());
        }
        if (request.getRatePerKgManioc() != null) {
            employee.setRatePerKgManioc(request.getRatePerKgManioc());
        }
        employee.setPosition(request.getPosition());
        if (request.getIsActive() != null) {
            employee.setIsActive(request.getIsActive());
        }
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
            String employeeId, LocalDateTime from, LocalDateTime to, String type, Pageable pageable) {
        findEmployeeById(employeeId);
        Page<EmployeeTransactionResponse> page = employeeTransactionRepository
                .findAll(transactionSpec(employeeId, from, to, type), pageable)
                .map(EmployeeTransactionMapper::toResponse);
        return PageResponse.from(page);
    }

    public EmployeeTransactionStatsResponse getEmployeeTransactionStats(
            String employeeId, LocalDateTime from, LocalDateTime to) {
        findEmployeeById(employeeId);
        List<EmployeeTransaction> records = employeeTransactionRepository.findAll(transactionSpec(employeeId, from, to, null));

        BigDecimal total = sumAmounts(records);
        BigDecimal manualLabor = sumAmountsByType(records, "Manual_Labor");
        BigDecimal advances = sumAmountsByType(records, "Advance");
        BigDecimal loanPayments = sumAmountsByType(records, "Loan_Payment");
        BigDecimal latexTap = sumAmountsByType(records, "Latex_Tap");
        BigDecimal harvestEarnings = sumAmountsByType(records, "Banana_Harvest")
                .add(sumAmountsByType(records, "Coconut_Harvest"))
                .add(sumAmountsByType(records, "Manioc_Harvest"));

        return EmployeeTransactionStatsResponse.builder()
                .totalAmount(total)
                .manualLabor(manualLabor)
                .advances(advances)
                .loanPayments(loanPayments)
                .latexTap(latexTap)
                .harvestEarnings(harvestEarnings)
                .transactionCount(records.size())
                .build();
    }

    private BigDecimal sumAmounts(List<EmployeeTransaction> records) {
        return records.stream()
                .map(EmployeeTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmountsByType(List<EmployeeTransaction> records, String type) {
        return records.stream()
                .filter(t -> type.equals(t.getType()))
                .map(EmployeeTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Employee attendance
    public PageResponse<AttendanceResponse> getEmployeeAttendance(
            String employeeId, LocalDateTime from, LocalDateTime to, String status, Pageable pageable) {
        findEmployeeById(employeeId);
        Page<AttendanceResponse> page = attendanceRepository
                .findAll(attendanceSpec(employeeId, from, to, status), pageable)
                .map(AttendanceMapper::toResponse);
        return PageResponse.from(page);
    }

    public List<AttendanceResponse> getEmployeeAttendanceByDateRange(
            String employeeId, LocalDateTime from, LocalDateTime to) {
        findEmployeeById(employeeId);
        return attendanceRepository.findByEmployee_EmployeeIdAndTimestampBetween(employeeId, from, to)
                .stream().map(AttendanceMapper::toResponse).collect(Collectors.toList());
    }

    public EmployeeAttendanceStatsResponse getEmployeeAttendanceStats(
            String employeeId, LocalDateTime from, LocalDateTime to) {
        findEmployeeById(employeeId);
        List<Attendance> records = attendanceRepository.findAll(attendanceSpec(employeeId, from, to, null));

        long total = records.size();
        long present = records.stream().filter(a -> "none".equals(a.getNoWork())).count();
        long absent = total - present;
        long totalTrees = records.stream()
                .mapToLong(a -> a.getNoOfTrees() != null ? a.getNoOfTrees() : 0)
                .sum();
        double avgTrees = present > 0 ? Math.round(((double) totalTrees / present) * 100) / 100.0 : 0;
        double rate = total > 0 ? Math.round((present * 10000.0 / total)) / 100.0 : 0;

        return EmployeeAttendanceStatsResponse.builder()
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .totalTreesTapped(totalTrees)
                .avgTreesPerPresentDay(avgTrees)
                .attendanceRatePercent(rate)
                .build();
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private Specification<Attendance> attendanceSpec(
            String employeeId, LocalDateTime from, LocalDateTime to, String status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("employee").get("employeeId"), employeeId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("noWork"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Specification<EmployeeTransaction> transactionSpec(
            String employeeId, LocalDateTime from, LocalDateTime to, String type) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("employee").get("employeeId"), employeeId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // Payment summary (Payment tab KPI cards)
    public PaymentSummaryResponse getPaymentSummary() {
        BigDecimal totalSalaryCost = employeeRepository.findByIsActiveTrue().stream()
                .map(Employee::getSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        BigDecimal toBePaid = labourRepository.findByIsPaidAndTimestampBetween(false, startOfMonth, endOfMonth)
                .stream().map(Labour::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal paidAmount = labourRepository.findByIsPaidAndTimestampBetween(true, startOfMonth, endOfMonth)
                .stream().map(Labour::getAmount).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal activeEmployeeLoansTotal = employeeLoanRepository.findByIsActiveTrue().stream()
                .map(EmployeeLoan::getCurrentBalance)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentSummaryResponse.builder()
                .totalSalaryCost(totalSalaryCost)
                .toBePaid(toBePaid)
                .paidAmount(paidAmount)
                .activeEmployeeLoansTotal(activeEmployeeLoansTotal)
                .build();
    }
}
