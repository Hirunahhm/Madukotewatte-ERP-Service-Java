package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.banana.BananaLoadSummaryResponse;
import com.madukotawatte.erp.dto.banana.BananaRecordRequest;
import com.madukotawatte.erp.dto.banana.BananaRecordResponse;
import com.madukotawatte.erp.dto.banana.BananaVarietyBreakdownResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.load.VolumeTrendResponse;
import com.madukotawatte.erp.entity.BananaRecord;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.BananaMapper;
import com.madukotawatte.erp.repository.BananaRecordRepository;
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
public class BananaProductionService {

    private final BananaRecordRepository bananaRecordRepository;
    private final LoadRepository loadRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;

    @Transactional
    public BananaRecordResponse createBananaRecord(BananaRecordRequest request) {
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        BananaRecord record = BananaMapper.toEntity(request, load, employee);
        record.setEmployeeTransaction(
                createHarvestTransactionIfApplicable(employee, request.getBunchCount(), request.getTimestamp()));
        return BananaMapper.toResponse(bananaRecordRepository.save(record));
    }

    public BananaRecordResponse getBananaRecord(String id) {
        return BananaMapper.toResponse(findBananaRecordById(id));
    }

    public List<BananaRecordResponse> getBananaRecordsByLoad(String loadId) {
        findLoadById(loadId);
        return bananaRecordRepository.findByLoad_LoadId(loadId).stream()
                .map(BananaMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<BananaRecordResponse> getAllBananaRecords(
            String variety, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<BananaRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (variety != null && !variety.isBlank()) predicates.add(cb.equal(root.get("variety"), variety));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(bananaRecordRepository.findAll(spec, pageable).map(BananaMapper::toResponse));
    }

    @Transactional
    public BananaRecordResponse updateBananaRecord(String id, BananaRecordRequest request) {
        BananaRecord record = findBananaRecordById(id);
        Load load = findLoadById(request.getLoadId());
        Employee employee = findEmployeeById(request.getEmployeeId());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setBunchCount(request.getBunchCount());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        if (record.getEmployeeTransaction() == null) {
            record.setEmployeeTransaction(
                    createHarvestTransactionIfApplicable(employee, request.getBunchCount(), request.getTimestamp()));
        }
        return BananaMapper.toResponse(bananaRecordRepository.save(record));
    }

    @Transactional
    public void deleteBananaRecord(String id) {
        bananaRecordRepository.delete(findBananaRecordById(id));
    }

    public BananaLoadSummaryResponse getBananaLoadSummary(String loadId) {
        Load load = findLoadById(loadId);
        List<BananaRecord> records = bananaRecordRepository.findByLoad_LoadId(loadId);
        BigDecimal totalMassKg = records.stream()
                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalBunchCount = records.stream()
                .mapToInt(r -> r.getBunchCount() != null ? r.getBunchCount() : 0)
                .sum();
        LocalDateTime lastCollectionAt = records.stream()
                .map(BananaRecord::getTimestamp)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return BananaLoadSummaryResponse.builder()
                .loadId(load.getLoadId())
                .loadType(load.getLoadType())
                .status(load.getStatus())
                .startDate(load.getStartDate())
                .totalMassKg(totalMassKg)
                .totalBunchCount(totalBunchCount)
                .recordCount(records.size())
                .lastCollectionAt(lastCollectionAt)
                .build();
    }

    public List<VolumeTrendResponse> getBananaLoadTrends(String loadId, int days) {
        findLoadById(loadId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<BananaRecord> records = bananaRecordRepository.findByLoad_LoadId(loadId).stream()
                .filter(r -> r.getTimestamp() != null && !r.getTimestamp().isBefore(cutoff))
                .sorted(Comparator.comparing(BananaRecord::getTimestamp))
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

    public List<BananaVarietyBreakdownResponse> getVarietyBreakdown(LocalDateTime from, LocalDateTime to) {
        Specification<BananaRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<BananaRecord> records = bananaRecordRepository.findAll(spec);

        Map<String, List<BananaRecord>> byVariety = records.stream()
                .collect(Collectors.groupingBy(BananaRecord::getVariety, LinkedHashMap::new, Collectors.toList()));

        return byVariety.entrySet().stream()
                .map(e -> BananaVarietyBreakdownResponse.builder()
                        .variety(e.getKey())
                        .totalMassKg(e.getValue().stream()
                                .map(r -> r.getMassKg() != null ? r.getMassKg() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .totalBunchCount(e.getValue().stream()
                                .mapToInt(r -> r.getBunchCount() != null ? r.getBunchCount() : 0)
                                .sum())
                        .recordCount(e.getValue().size())
                        .build())
                .collect(Collectors.toList());
    }

    private EmployeeTransaction createHarvestTransactionIfApplicable(Employee employee, Integer bunchCount, LocalDateTime timestamp) {
        if (bunchCount == null || bunchCount <= 0) return null;
        BigDecimal ratePerBunch = employee.getRatePerBunch();
        if (ratePerBunch == null || ratePerBunch.compareTo(BigDecimal.ZERO) <= 0) return null;

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(UUID.randomUUID().toString())
                .employee(employee)
                .type("Banana_Harvest")
                .amount(ratePerBunch.multiply(BigDecimal.valueOf(bunchCount)))
                .timestamp(timestamp)
                .build();
        return employeeTransactionRepository.save(transaction);
    }

    private BananaRecord findBananaRecordById(String id) {
        return bananaRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BananaRecord", "id", id));
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
