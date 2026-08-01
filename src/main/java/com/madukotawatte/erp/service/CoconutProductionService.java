package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.coconut.CoconutLoadSummaryResponse;
import com.madukotawatte.erp.dto.coconut.CoconutRecordRequest;
import com.madukotawatte.erp.dto.coconut.CoconutRecordResponse;
import com.madukotawatte.erp.dto.coconut.CoconutVarietyBreakdownResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.entity.CoconutRecord;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.CoconutMapper;
import com.madukotawatte.erp.repository.CoconutRecordRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import com.madukotawatte.erp.repository.LoadRepository;
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
public class CoconutProductionService {

    private final CoconutRecordRepository coconutRecordRepository;
    private final LoadRepository loadRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;

    @Transactional
    public CoconutRecordResponse createCoconutRecord(CoconutRecordRequest request) {
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        CoconutRecord record = CoconutMapper.toEntity(request, load, employee);
        record.setEmployeeTransaction(
                createHarvestTransactionIfApplicable(employee, request.getNutCount(), request.getTimestamp()));
        return CoconutMapper.toResponse(coconutRecordRepository.save(record));
    }

    public CoconutRecordResponse getCoconutRecord(String id) {
        return CoconutMapper.toResponse(findCoconutRecordById(id));
    }

    public List<CoconutRecordResponse> getCoconutRecordsByLoad(String loadId) {
        findLoadById(loadId);
        return coconutRecordRepository.findByLoad_LoadId(loadId).stream()
                .map(CoconutMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<CoconutRecordResponse> getAllCoconutRecords(
            String variety, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<CoconutRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variety != null && !variety.isBlank()) predicates.add(cb.equal(root.get("variety"), variety));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(coconutRecordRepository.findAll(spec, pageable).map(CoconutMapper::toResponse));
    }

    @Transactional
    public CoconutRecordResponse updateCoconutRecord(String id, CoconutRecordRequest request) {
        CoconutRecord record = findCoconutRecordById(id);
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setNutCount(request.getNutCount());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        if (record.getEmployeeTransaction() == null) {
            record.setEmployeeTransaction(
                    createHarvestTransactionIfApplicable(employee, request.getNutCount(), request.getTimestamp()));
        }
        return CoconutMapper.toResponse(coconutRecordRepository.save(record));
    }

    @Transactional
    public void deleteCoconutRecord(String id) {
        coconutRecordRepository.delete(findCoconutRecordById(id));
    }

    public CoconutLoadSummaryResponse getCoconutLoadSummary(String loadId) {
        Load load = findLoadById(loadId);
        List<CoconutRecord> records = coconutRecordRepository.findByLoad_LoadId(loadId);
        BigDecimal totalMassKg = records.stream()
                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalNutCount = records.stream()
                .mapToInt(r -> r.getNutCount() != null ? r.getNutCount() : 0)
                .sum();
        LocalDateTime lastCollectionAt = records.stream()
                .map(CoconutRecord::getTimestamp)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return CoconutLoadSummaryResponse.builder()
                .loadId(load.getLoadId())
                .loadType(load.getLoadType())
                .status(load.getStatus())
                .startDate(load.getStartDate())
                .totalMassKg(totalMassKg)
                .totalNutCount(totalNutCount)
                .recordCount(records.size())
                .lastCollectionAt(lastCollectionAt)
                .build();
    }

    public List<VolumeTrendResponse> getCoconutLoadTrends(String loadId, int days) {
        findLoadById(loadId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<CoconutRecord> records = coconutRecordRepository.findByLoad_LoadId(loadId).stream()
                .filter(r -> r.getTimestamp() != null && !r.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(CoconutRecord::getTimestamp))
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

    public List<CoconutVarietyBreakdownResponse> getVarietyBreakdown(LocalDateTime from, LocalDateTime to) {
        Specification<CoconutRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<CoconutRecord> records = coconutRecordRepository.findAll(spec);

        Map<String, List<CoconutRecord>> byVariety = records.stream()
                .collect(Collectors.groupingBy(CoconutRecord::getVariety, LinkedHashMap::new, Collectors.toList()));

        return byVariety.entrySet().stream()
                .map(e -> CoconutVarietyBreakdownResponse.builder()
                        .variety(e.getKey())
                        .totalMassKg(e.getValue().stream()
                                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .totalNutCount(e.getValue().stream()
                                .mapToInt(r -> r.getNutCount() != null ? r.getNutCount() : 0)
                                .sum())
                        .recordCount(e.getValue().size())
                        .build())
                .collect(Collectors.toList());
    }

    private EmployeeTransaction createHarvestTransactionIfApplicable(Employee employee, Integer nutCount, LocalDateTime timestamp) {
        if (nutCount == null || nutCount <= 0) return null;
        BigDecimal ratePerNut = employee.getRatePerNut();
        if (ratePerNut == null || ratePerNut.compareTo(BigDecimal.ZERO) <= 0) return null;

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type("Coconut_Harvest")
                .amount(ratePerNut.multiply(BigDecimal.valueOf(nutCount)))
                .timestamp(timestamp)
                .build();
        return employeeTransactionRepository.save(transaction);
    }

    private CoconutRecord findCoconutRecordById(String id) {
        return coconutRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CoconutRecord", "id", id));
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
