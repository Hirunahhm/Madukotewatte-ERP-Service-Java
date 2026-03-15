package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordRequest;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordResponse;
import com.madukotawatte.erp.dto.attendance.AttendanceBulkRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.latex.LatexRecordRequest;
import com.madukotawatte.erp.dto.latex.LatexRecordResponse;
import com.madukotawatte.erp.dto.load.LoadRequest;
import com.madukotawatte.erp.dto.load.LoadResponse;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingRequest;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordRequest;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
import com.madukotawatte.erp.entity.*;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.*;
import com.madukotawatte.erp.repository.*;
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
public class DailyOperationsService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LoadRepository loadRepository;
    private final LatexRecordRepository latexRecordRepository;
    private final MetrolacReadingRepository metrolacReadingRepository;
    private final AmmoniaRecordRepository ammoniaRecordRepository;
    private final RubberSolidRecordRepository rubberSolidRecordRepository;
    private final SalesLatexRepository salesLatexRepository;

    // ── Attendance ──────────────────────────────────────────────
    @Transactional
    public AttendanceResponse createAttendance(AttendanceRequest request) {
        Employee employee = findEmployeeById(request.getEmployeeId());
        Attendance attendance = AttendanceMapper.toEntity(request, employee);
        return AttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public List<AttendanceResponse> createAttendanceBulk(AttendanceBulkRequest request) {
        return request.getAttendances().stream()
                .map(this::createAttendance)
                .collect(Collectors.toList());
    }

    public AttendanceResponse getAttendance(String id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
        return AttendanceMapper.toResponse(attendance);
    }

    public PageResponse<AttendanceResponse> getAllAttendances(Pageable pageable) {
        return PageResponse.from(attendanceRepository.findAll(pageable).map(AttendanceMapper::toResponse));
    }

    public List<AttendanceResponse> getAttendanceByDateRange(LocalDateTime from, LocalDateTime to) {
        return attendanceRepository.findByTimestampBetween(from, to).stream()
                .map(AttendanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceResponse updateAttendance(String id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
        Employee employee = findEmployeeById(request.getEmployeeId());
        attendance.setEmployee(employee);
        attendance.setTimestamp(request.getTimestamp());
        attendance.setNoOfTrees(request.getNoOfTrees());
        attendance.setNoWork(request.getNoWork() != null ? request.getNoWork() : "none");
        return AttendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public void deleteAttendance(String id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));
        attendanceRepository.delete(attendance);
    }

    // ── Load ────────────────────────────────────────────────────
    @Transactional
    public LoadResponse createLoad(LoadRequest request) {
        Load load = LoadMapper.toEntity(request);
        return LoadMapper.toResponse(loadRepository.save(load));
    }

    public LoadResponse getLoad(String id) {
        return LoadMapper.toResponse(findLoadById(id));
    }

    public PageResponse<LoadResponse> getAllLoads(Pageable pageable) {
        return PageResponse.from(loadRepository.findAll(pageable).map(LoadMapper::toResponse));
    }

    public PageResponse<LoadResponse> getLoadsByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return PageResponse.from(loadRepository.findByStartDateBetween(from, to, pageable).map(LoadMapper::toResponse));
    }

    @Transactional
    public LoadResponse updateLoad(String id, LoadRequest request) {
        Load load = findLoadById(id);
        load.setLoadType(request.getLoadType());
        load.setStartDate(request.getStartDate());
        load.setEndDate(request.getEndDate());
        return LoadMapper.toResponse(loadRepository.save(load));
    }

    @Transactional
    public void deleteLoad(String id) {
        Load load = findLoadById(id);
        if (!latexRecordRepository.findByLoad_LoadId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete load with associated latex records");
        }
        if (!rubberSolidRecordRepository.findByLoad_LoadId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete load with associated rubber solid records");
        }
        if (!salesLatexRepository.findByLoad_LoadId(id).isEmpty()) {
            throw new BadRequestException("Cannot delete load with associated sales records");
        }
        loadRepository.delete(load);
    }

    // ── Latex Records ───────────────────────────────────────────
    @Transactional
    public LatexRecordResponse createLatexRecord(LatexRecordRequest request) {
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        MetrolacReading metrolacReading = null;
        if (request.getMetrolacReadingId() != null) {
            metrolacReading = metrolacReadingRepository.findById(request.getMetrolacReadingId())
                    .orElseThrow(() -> new ResourceNotFoundException("MetrolacReading", "id", request.getMetrolacReadingId()));
        }
        LatexRecord record = LatexRecordMapper.toEntity(request, load, employee, metrolacReading);
        return LatexRecordMapper.toResponse(latexRecordRepository.save(record));
    }

    public LatexRecordResponse getLatexRecord(String id) {
        return LatexRecordMapper.toResponse(latexRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LatexRecord", "id", id)));
    }

    public List<LatexRecordResponse> getLatexRecordsByLoad(String loadId) {
        findLoadById(loadId);
        return latexRecordRepository.findByLoad_LoadId(loadId).stream()
                .map(LatexRecordMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<LatexRecordResponse> getAllLatexRecords(Pageable pageable) {
        return PageResponse.from(latexRecordRepository.findAll(pageable).map(LatexRecordMapper::toResponse));
    }

    @Transactional
    public LatexRecordResponse updateLatexRecord(String id, LatexRecordRequest request) {
        LatexRecord record = latexRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LatexRecord", "id", id));
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        MetrolacReading metrolacReading = null;
        if (request.getMetrolacReadingId() != null) {
            metrolacReading = metrolacReadingRepository.findById(request.getMetrolacReadingId())
                    .orElseThrow(() -> new ResourceNotFoundException("MetrolacReading", "id", request.getMetrolacReadingId()));
        }
        record.setLoad(load);
        record.setEmployee(employee);
        record.setTimestamp(request.getTimestamp());
        record.setLatexAmount(request.getLatexAmount());
        record.setAmmoniaAmount(request.getAmmoniaAmount());
        record.setMetrolacReading(metrolacReading);
        return LatexRecordMapper.toResponse(latexRecordRepository.save(record));
    }

    @Transactional
    public void deleteLatexRecord(String id) {
        LatexRecord record = latexRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LatexRecord", "id", id));
        latexRecordRepository.delete(record);
    }

    // ── Metrolac Readings ───────────────────────────────────────
    @Transactional
    public MetrolacReadingResponse createMetrolacReading(MetrolacReadingRequest request) {
        Load load = findLoadById(request.getLoadId());
        MetrolacReading reading = MetrolacMapper.toEntity(request, load);
        return MetrolacMapper.toResponse(metrolacReadingRepository.save(reading));
    }

    public MetrolacReadingResponse getMetrolacReading(String id) {
        return MetrolacMapper.toResponse(metrolacReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MetrolacReading", "id", id)));
    }

    public List<MetrolacReadingResponse> getMetrolacReadingsByLoad(String loadId) {
        findLoadById(loadId);
        return metrolacReadingRepository.findByLoad_LoadId(loadId).stream()
                .map(MetrolacMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<MetrolacReadingResponse> getAllMetrolacReadings(Pageable pageable) {
        return PageResponse.from(metrolacReadingRepository.findAll(pageable).map(MetrolacMapper::toResponse));
    }

    @Transactional
    public MetrolacReadingResponse updateMetrolacReading(String id, MetrolacReadingRequest request) {
        MetrolacReading reading = metrolacReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MetrolacReading", "id", id));
        Load load = findLoadById(request.getLoadId());
        reading.setLoad(load);
        reading.setTemperature(request.getTemperature());
        reading.setTimestamp(request.getTimestamp());
        return MetrolacMapper.toResponse(metrolacReadingRepository.save(reading));
    }

    @Transactional
    public void deleteMetrolacReading(String id) {
        MetrolacReading reading = metrolacReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MetrolacReading", "id", id));
        metrolacReadingRepository.delete(reading);
    }

    // ── Ammonia Records ─────────────────────────────────────────
    @Transactional
    public AmmoniaRecordResponse createAmmoniaRecord(AmmoniaRecordRequest request) {
        AmmoniaRecord record = AmmoniaMapper.toEntity(request);
        return AmmoniaMapper.toResponse(ammoniaRecordRepository.save(record));
    }

    public PageResponse<AmmoniaRecordResponse> getAllAmmoniaRecords(Pageable pageable) {
        return PageResponse.from(ammoniaRecordRepository.findAll(pageable).map(AmmoniaMapper::toResponse));
    }

    // ── Rubber Solid Records ────────────────────────────────────
    @Transactional
    public RubberSolidRecordResponse createRubberSolidRecord(RubberSolidRecordRequest request) {
        Load load = findLoadById(request.getLoadId());
        RubberSolidRecord record = RubberSolidMapper.toEntity(request, load);
        return RubberSolidMapper.toResponse(rubberSolidRecordRepository.save(record));
    }

    public List<RubberSolidRecordResponse> getRubberSolidRecordsByLoad(String loadId) {
        findLoadById(loadId);
        return rubberSolidRecordRepository.findByLoad_LoadId(loadId).stream()
                .map(RubberSolidMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<RubberSolidRecordResponse> getAllRubberSolidRecords(Pageable pageable) {
        return PageResponse.from(rubberSolidRecordRepository.findAll(pageable).map(RubberSolidMapper::toResponse));
    }

    // ── Helpers ─────────────────────────────────────────────────
    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private Load findLoadById(String id) {
        return loadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", id));
    }
}
