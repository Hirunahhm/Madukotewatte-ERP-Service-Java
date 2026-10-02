package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.attendance.AttendanceBulkRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.entity.Attendance;
import com.madukotawatte.erp.entity.Calendar;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.AttendanceMapper;
import com.madukotawatte.erp.repository.AttendanceRepository;
import com.madukotawatte.erp.repository.CalendarRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final String LATEX_TAP = "Latex_Tap";

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CalendarRepository calendarRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;

    @Transactional
    public AttendanceResponse createAttendance(AttendanceRequest request) {
        return createAttendanceInternal(request);
    }

    /** All records share one transaction; uses the private helper so the proxy-bypass does not matter. */
    @Transactional
    public List<AttendanceResponse> createAttendanceBulk(AttendanceBulkRequest request) {
        return request.getAttendances().stream()
                .map(this::createAttendanceInternal)
                .collect(Collectors.toList());
    }

    private AttendanceResponse createAttendanceInternal(AttendanceRequest request) {
        Employee employee = findEmployeeById(request.getEmployeeId());
        Calendar calendar = findCalendar(request.getCalendarId());
        Attendance attendance = AttendanceMapper.toEntity(request, employee, calendar);
        attendance.setEmployeeTransaction(
                createLatexTapTransactionIfApplicable(employee, request.getNoOfTrees(), attendance.getTimestamp()));
        return AttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    public AttendanceResponse getAttendance(String id) {
        return AttendanceMapper.toResponse(find(id));
    }

    public PageResponse<AttendanceResponse> getAllAttendances(Pageable pageable) {
        return PageResponse.from(attendanceRepository.findAll(pageable).map(AttendanceMapper::toResponse));
    }

    public List<AttendanceResponse> getAttendanceByDateRange(LocalDateTime from, LocalDateTime to) {
        return attendanceRepository.findByTimestampBetween(from, to).stream()
                .map(AttendanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    /** Keeps the linked Latex_Tap transaction in step with trees / employee / date (create, update or remove it). */
    @Transactional
    public AttendanceResponse updateAttendance(String id, AttendanceRequest request) {
        Attendance attendance = find(id);
        Employee employee = findEmployeeById(request.getEmployeeId());
        Calendar calendar = findCalendar(request.getCalendarId());

        boolean earningsChanged = !Objects.equals(attendance.getEmployee().getEmployeeId(), employee.getEmployeeId())
                || !Objects.equals(attendance.getNoOfTrees(), request.getNoOfTrees())
                || !Objects.equals(attendance.getTimestamp(), request.getTimestamp());

        attendance.setEmployee(employee);
        attendance.setCalendar(calendar);
        attendance.setTimestamp(request.getTimestamp());
        attendance.setNoOfTrees(request.getNoOfTrees());
        attendance.setNoWork(request.getNoWork() != null ? request.getNoWork() : "none");

        EmployeeTransaction existing = attendance.getEmployeeTransaction();
        if (existing == null) {
            attendance.setEmployeeTransaction(
                    createLatexTapTransactionIfApplicable(employee, request.getNoOfTrees(), request.getTimestamp()));
        } else if (earningsChanged) {
            BigDecimal amount = latexTapAmount(employee, request.getNoOfTrees());
            if (amount == null) {
                attendance.setEmployeeTransaction(null);
                attendanceRepository.saveAndFlush(attendance);
                employeeTransactionRepository.delete(existing);
            } else {
                existing.setEmployee(employee);
                existing.setAmount(amount);
                existing.setTimestamp(request.getTimestamp());
                employeeTransactionRepository.save(existing);
            }
        }
        return AttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    /** Deleting attendance also deletes the Latex_Tap earning it created. */
    @Transactional
    public void deleteAttendance(String id) {
        Attendance attendance = find(id);
        EmployeeTransaction tapTransaction = attendance.getEmployeeTransaction();
        attendanceRepository.delete(attendance);
        if (tapTransaction != null && LATEX_TAP.equals(tapTransaction.getType())) {
            attendanceRepository.flush();
            employeeTransactionRepository.delete(tapTransaction);
        }
    }

    /**
     * Trees tapped during attendance are a real earning, not just a productivity stat: this posts a Latex_Tap
     * transaction (employee.ratePerTree x trees) that feeds into the monthly payroll. The rate defaults to 0, so
     * nothing is posted until an admin sets a per-tree rate on the employee.
     */
    private EmployeeTransaction createLatexTapTransactionIfApplicable(Employee employee, Integer noOfTrees, LocalDateTime timestamp) {
        BigDecimal amount = latexTapAmount(employee, noOfTrees);
        if (amount == null) return null;

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type(LATEX_TAP)
                .amount(amount)
                .timestamp(timestamp)
                .build();
        return employeeTransactionRepository.save(transaction);
    }

    private BigDecimal latexTapAmount(Employee employee, Integer noOfTrees) {
        if (noOfTrees == null || noOfTrees <= 0) return null;
        BigDecimal ratePerTree = employee.getRatePerTree();
        if (ratePerTree == null || ratePerTree.compareTo(BigDecimal.ZERO) <= 0) return null;
        return ratePerTree.multiply(BigDecimal.valueOf(noOfTrees));
    }

    private Attendance find(String id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
    }

    private Calendar findCalendar(String calendarId) {
        return calendarId != null
                ? calendarRepository.findById(calendarId)
                        .orElseThrow(() -> new ResourceNotFoundException("Calendar", "id", calendarId))
                : null;
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
