package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetSummaryResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetUpdateRequest;
import com.madukotawatte.erp.entity.FixedAsset;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.FixedAssetMapper;
import com.madukotawatte.erp.repository.FixedAssetRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;

    @Transactional
    public FixedAssetResponse createFixedAsset(FixedAssetRequest request) {
        FixedAsset asset = FixedAsset.builder()
                .assetId(UUID.randomUUID().toString())
                .category(request.getCategory())
                .name(request.getName())
                .acquisitionDate(request.getAcquisitionDate())
                .acquisitionValue(request.getAcquisitionValue())
                .currentValue(request.getCurrentValue() != null ? request.getCurrentValue() : request.getAcquisitionValue())
                .status("active")
                .location(request.getLocation())
                .notes(request.getNotes())
                .build();
        return FixedAssetMapper.toResponse(fixedAssetRepository.save(asset));
    }

    public PageResponse<FixedAssetResponse> getAllFixedAssets(String category, String status, Pageable pageable) {
        return PageResponse.from(fixedAssetRepository.findAll(fixedAssetSpec(category, status), pageable).map(FixedAssetMapper::toResponse));
    }

    @Transactional
    public FixedAssetResponse updateFixedAsset(String id, FixedAssetUpdateRequest request) {
        FixedAsset asset = fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FixedAsset", "id", id));
        if (request.getCurrentValue() != null) {
            asset.setCurrentValue(request.getCurrentValue());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            asset.setStatus(request.getStatus());
        }
        if (request.getLocation() != null) {
            asset.setLocation(request.getLocation());
        }
        if (request.getNotes() != null) {
            asset.setNotes(request.getNotes());
        }
        return FixedAssetMapper.toResponse(fixedAssetRepository.save(asset));
    }

    public FixedAssetSummaryResponse getFixedAssetSummary() {
        List<FixedAsset> assets = fixedAssetRepository.findAll(fixedAssetSpec(null, "active"));

        BigDecimal totalAcquisitionValue = assets.stream()
                .map(FixedAsset::getAcquisitionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrentValue = assets.stream()
                .map(FixedAsset::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryTotalResponse> byCategory = assets.stream()
                .collect(Collectors.groupingBy(FixedAsset::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, FixedAsset::getCurrentValue, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> CategoryTotalResponse.builder().category(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());

        return FixedAssetSummaryResponse.builder()
                .totalCount(assets.size())
                .totalAcquisitionValue(totalAcquisitionValue)
                .totalCurrentValue(totalCurrentValue)
                .byCategory(byCategory)
                .build();
    }

    private Specification<FixedAsset> fixedAssetSpec(String category, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
