package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordRequest;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordResponse;
import com.madukotawatte.erp.dto.ammonia.AmmoniaBalanceResponse;
import com.madukotawatte.erp.dto.ammonia.AmmoniaUsageResponse;
import com.madukotawatte.erp.dto.calendar.CalendarRequest;
import com.madukotawatte.erp.dto.calendar.CalendarResponse;
import com.madukotawatte.erp.dto.attendance.AttendanceBulkRequest;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.labour.LabourRequest;
import com.madukotawatte.erp.dto.labour.LabourResponse;
import com.madukotawatte.erp.dto.latex.LatexRecordRequest;
import com.madukotawatte.erp.dto.latex.LatexRecordResponse;
import com.madukotawatte.erp.dto.load.LoadRequest;
import com.madukotawatte.erp.dto.load.LoadResponse;
import com.madukotawatte.erp.dto.load.LoadSummaryResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingRequest;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordRequest;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberLoadSummaryResponse;
import com.madukotawatte.erp.entity.*;
import com.madukotawatte.erp.repository.CalendarRepository;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.*;
import com.madukotawatte.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    // ── Calendar Operations ─────────────────────────────────────
    @Transactional
    public CalendarResponse createOrUpdateCalendar(CalendarRequest request) {
        LocalDate date = request.getCalendarDate();
        Calendar calendar = calendarRepository.findByCalendarDate(date)
                .orElse(null);

        if (calendar == null) {
            calendar = CalendarMapper.toEntity(request);
        } else {
            calendar.setIsHoliday(request.getIsHoliday() != null ? request.getIsHoliday() : false);
            calendar.setRained(request.getRained() != null ? request.getRained() : false);
            calendar.setIsWorking(request.getIsWorking() != null ? request.getIsWorking() : true);
            calendar.setDescription(request.getDescription());
        }

        Calendar savedCalendar = calendarRepository.save(calendar);

        // Propagation logic: If non-working day, auto-log default attendance
        if (Boolean.FALSE.equals(savedCalendar.getIsWorking()) || 
            Boolean.TRUE.equals(savedCalendar.getIsHoliday()) || 
            Boolean.TRUE.equals(savedCalendar.getRained())) {
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

            // 1. Get IDs of employees who already have attendance logged for this date.
            List<Attendance> existingLogs = attendanceRepository.findByTimestampBetween(startOfDay, endOfDay);
            Set<String> alreadyLoggedEmployeeIds = existingLogs.stream()
                    .map(a -> a.getEmployee().getEmployeeId())
                    .collect(Collectors.toSet());

            // 2. Fetch all active employees.
            List<Employee> activeEmployees = employeeRepository.findByIsActiveTrue();

            // 3. For any active employee without attendance, generate a default "no-work" record.
            String noWorkReason = "none";
            if (Boolean.TRUE.equals(savedCalendar.getIsHoliday())) {
                noWorkReason = "public_holiday";
            } else if (Boolean.TRUE.equals(savedCalendar.getRained())) {
                noWorkReason = "rain";
            } else if (Boolean.FALSE.equals(savedCalendar.getIsWorking())) {
                noWorkReason = "public_holiday"; // default fallback for general non-working day
            }

            for (Employee employee : activeEmployees) {
                if (!alreadyLoggedEmployeeIds.contains(employee.getEmployeeId())) {
                    Attendance attendance = new Attendance();
                    attendance.setAttendanceId(UUID.randomUUID().toString());
                    attendance.setEmployee(employee);
                    attendance.setCalendar(savedCalendar);
                    attendance.setTimestamp(startOfDay); // set default daily timestamp (start of day)
                    attendance.setNoOfTrees(0);
                    attendance.setNoWork(noWorkReason);
                    attendanceRepository.save(attendance);
                }
            }
        }

        return CalendarMapper.toResponse(savedCalendar);
    }

    public CalendarResponse getCalendarByDate(LocalDate date) {
        Calendar calendar = calendarRepository.findByCalendarDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar", "date", date.toString()));
        return CalendarMapper.toResponse(calendar);
    }

    public PageResponse<CalendarResponse> getAllCalendars(Pageable pageable) {
        return PageResponse.from(calendarRepository.findAll(pageable).map(CalendarMapper::toResponse));
    }
}
