package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestRequest;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestResponse;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestSummaryRow;
import com.madukotawatte.erp.entity.CropHarvest;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.CropHarvestMapper;
import com.madukotawatte.erp.repository.CropHarvestRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.service.support.ProductCatalog;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Harvest records for cocoa and other minor crops. */
@Service
@RequiredArgsConstructor
public class CropHarvestService {

    private final CropHarvestRepository cropHarvestRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public CropHarvestResponse create(CropHarvestRequest request) {
        validate(request);
        CropHarvest harvest = CropHarvestMapper.toEntity(request, findEmployee(request.getEmployeeId()));
        return CropHarvestMapper.toResponse(cropHarvestRepository.save(harvest));
    }

    @Transactional(readOnly = true)
    public CropHarvestResponse get(String id) {
        return CropHarvestMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CropHarvestResponse> getAll(String crop, String fieldBlock, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<CropHarvest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (crop != null && !crop.isBlank()) {
                predicates.add(cb.equal(root.get("crop"), crop));
            }
            if (fieldBlock != null && !fieldBlock.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("fieldBlock")), "%" + fieldBlock.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("harvestDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("harvestDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(cropHarvestRepository.findAll(spec, pageable).map(CropHarvestMapper::toResponse));
    }

    @Transactional
    public CropHarvestResponse update(String id, CropHarvestRequest request) {
        validate(request);
        CropHarvest harvest = find(id);
        CropHarvestMapper.applyFields(harvest, request, findEmployee(request.getEmployeeId()));
        return CropHarvestMapper.toResponse(cropHarvestRepository.save(harvest));
    }

    @Transactional
    public void delete(String id) {
        cropHarvestRepository.delete(find(id));
    }

    /** Quantity per crop per month (and unit) within [from, to]; both bounds optional. */
    @Transactional(readOnly = true)
    public List<CropHarvestSummaryRow> summary(LocalDate from, LocalDate to) {
        return cropHarvestRepository.summarize(from, to).stream()
                .map(r -> CropHarvestSummaryRow.builder()
                        .crop((String) r[0])
                        .month((String) r[1])
                        .unit((String) r[2])
                        .quantity((BigDecimal) r[3])
                        .harvests(((Number) r[4]).longValue())
                        .build())
                .toList();
    }

    private void validate(CropHarvestRequest request) {
        ProductCatalog.require(ProductCatalog.CROPS, request.getCrop(), "crop");
        ProductCatalog.require(ProductCatalog.UNITS, request.getUnit(), "unit");
    }

    private Employee findEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return null;
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private CropHarvest find(String id) {
        return cropHarvestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CropHarvest", "id", id));
    }
}
