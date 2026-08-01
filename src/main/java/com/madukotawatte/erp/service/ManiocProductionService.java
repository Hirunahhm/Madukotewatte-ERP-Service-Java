package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.dto.manioc.ManiocLoadSummaryResponse;
import com.madukotawatte.erp.dto.manioc.ManiocRecordRequest;
import com.madukotawatte.erp.dto.manioc.ManiocRecordResponse;
import com.madukotawatte.erp.dto.manioc.ManiocVarietyBreakdownResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.ManiocRecord;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.ManiocMapper;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import com.madukotawatte.erp.repository.LoadRepository;
import com.madukotawatte.erp.repository.ManiocRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManiocProductionService {

    private final ManiocRecordRepository maniocRecordRepository;
    private final LoadRepository loadRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;

    @Transactional
    public ManiocRecordResponse createManiocRecord(ManiocRecordRequest request) {
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        ManiocRecord record = ManiocMapper.toEntity(request, load, employee);
        record.setEmployeeTransaction(
                createHarvestTransactionIfApplicable(employee, request.getMassKg(), request.getTimestamp()));
        return ManiocMapper.toResponse(maniocRecordRepository.save(record));
    }

    public ManiocRecordResponse getManiocRecord(String id) {
        return ManiocMapper.toResponse(findManiocRecordById(id));
    }

    public List<ManiocRecordResponse> getManiocRecordsByLoad(String loadId) {
        findLoadById(loadId);
        return maniocRecordRepository.findByLoad_LoadId(loadId).stream()
                .map(ManiocMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<ManiocRecordResponse> getAllManiocRecords(
            String variety, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<ManiocRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variety != null && !variety.isBlank()) predicates.add(cb.equal(root.get("variety"), variety));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(maniocRecordRepository.findAll(spec, pageable).map(ManiocMapper::toResponse));
    }

    @Transactional
    public ManiocRecordResponse updateManiocRecord(String id, ManiocRecordRequest request) {
        ManiocRecord record = findManiocRecordById(id);
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        if (record.getEmployeeTransaction() == null) {
            record.setEmployeeTransaction(
                    createHarvestTransactionIfApplicable(employee, request.getMassKg(), request.getTimestamp()));
        }
        return ManiocMapper.toResponse(maniocRecordRepository.save(record));
    }

    @Transactional
    public void deleteManiocRecord(String id) {
        maniocRecordRepository.delete(findManiocRecordById(id));
    }

    public ManiocLoadSummaryResponse getManiocLoadSummary(String loadId) {
        Load load = findLoadById(loadId);
        List<ManiocRecord> records = maniocRecordRepository.findByLoad_LoadId(loadId);
        BigDecimal totalMassKg = records.stream()
                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime lastCollectionAt = records.stream()
                .map(ManiocRecord::getTimestamp)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return ManiocLoadSummaryResponse.builder()
                .loadId(load.getLoadId())
                .loadType(load.getLoadType())
                .status(load.getStatus())
                .startDate(load.getStartDate())
                .totalMassKg(totalMassKg)
                .recordCount(records.size())
                .lastCollectionAt(lastCollectionAt)
                .build();
    }

    public List<VolumeTrendResponse> getManiocLoadTrends(String loadId, int days) {
        findLoadById(loadId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<ManiocRecord> records = maniocRecordRepository.findByLoad_LoadId(loadId).stream()
                .filter(r -> r.getTimestamp() != null && !r.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(ManiocRecord::getTimestamp))
                .collect(Collectors.toList());

        Map<String, BigDecimal> sortedTotals = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTimestamp().toLocalDate().format(DateTimeFormatter.ofPattern("MMM dd")),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return sortedTotals.entrySet().stream()
                .map(e -> VolumeTrendResponse.builder()
                        .name(e.getKey())
                        .actual(e.getValue())
                        .target(null)
                        .build())
                .collect(Collectors.toList());
    }

    public List<ManiocVarietyBreakdownResponse> getVarietyBreakdown(LocalDateTime from, LocalDateTime to) {
        Specification<ManiocRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<ManiocRecord> records = maniocRecordRepository.findAll(spec);

        Map<String, List<ManiocRecord>> byVariety = records.stream()
                .collect(Collectors.groupingBy(ManiocRecord::getVariety, LinkedHashMap::new, Collectors.toList()));

        return byVariety.entrySet().stream()
                .map(e -> ManiocVarietyBreakdownResponse.builder()
                        .variety(e.getKey())
                        .totalMassKg(e.getValue().stream()
                                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .recordCount(e.getValue().size())
                        .build())
                .collect(Collectors.toList());
    }

    private EmployeeTransaction createHarvestTransactionIfApplicable(Employee employee, BigDecimal massKg, LocalDateTime timestamp) {
        if (massKg == null || massKg.compareTo(BigDecimal.ZERO) <= 0) return null;
        BigDecimal ratePerKgManioc = employee.getRatePerKgManioc();
        if (ratePerKgManioc == null || ratePerKgManioc.compareTo(BigDecimal.ZERO) <= 0) return null;

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type("Manioc_Harvest")
                .amount(ratePerKgManioc.multiply(massKg))
                .timestamp(timestamp)
                .build();
        return employeeTransactionRepository.save(transaction);
    }

    private ManiocRecord findManiocRecordById(String id) {
        return maniocRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ManiocRecord", "id", id));
    }

    private Load findLoadById(String id) {
        return loadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", id));
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
