package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetDisposalRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetRequest;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetSummaryResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetUpdateRequest;
import com.madukotawatte.erp.entity.FixedAsset;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.FixedAssetMapper;
import com.madukotawatte.erp.repository.FixedAssetRepository;
import com.madukotawatte.erp.repository.MonetaryAssetTransactionRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fixed assets. Purchases may post ASSET_PURCHASE (ref FIXED_ASSET/assetId); assets without a purchase account are
 * pre-existing (they belong to opening equity). The current_value column is kept equal to the NBV at the last write
 * for SQL consumers; the API always returns the live computed NBV.
 */
@Service
@RequiredArgsConstructor
public class FixedAssetService {

    public static final String REF_FIXED_ASSET = "FIXED_ASSET";

    private final FixedAssetRepository fixedAssetRepository;
    private final LedgerService ledgerService;
    private final MonetaryAssetTransactionRepository ledgerRepository;

    @Transactional
    public FixedAssetResponse createFixedAsset(FixedAssetRequest request) {
        String method = request.getDepreciationMethod() != null && !request.getDepreciationMethod().isBlank()
                ? normaliseMethod(request.getDepreciationMethod())
                : DepreciationCalculator.defaultMethod(request.getCategory());
        Integer life = request.getUsefulLifeYears() != null
                ? request.getUsefulLifeYears()
                : DepreciationCalculator.defaultLifeYears(request.getCategory());
        if (DepreciationCalculator.STRAIGHT_LINE.equals(method) && life == null) {
            life = 5;
        }
        BigDecimal residual = request.getResidualValue() != null ? request.getResidualValue() : BigDecimal.ZERO;
        if (residual.compareTo(request.getAcquisitionValue()) > 0) {
            throw new BadRequestException("Residual value cannot exceed the acquisition value");
        }
        String paidFrom = blankToNull(request.getPaidFromAccountCode());
        if (paidFrom != null) {
            ledgerService.requireActiveAccount(paidFrom);
            if (request.getAcquisitionValue().signum() <= 0) {
                throw new BadRequestException("A purchase needs an acquisition value above zero");
            }
        }

        FixedAsset asset = FixedAsset.builder()
                .assetId(UUID.randomUUID().toString())
                .category(request.getCategory())
                .name(request.getName())
                .acquisitionDate(request.getAcquisitionDate())
                .acquisitionValue(request.getAcquisitionValue())
                .currentValue(request.getAcquisitionValue())
                .status("active")
                .location(request.getLocation())
                .notes(request.getNotes())
                .usefulLifeYears(DepreciationCalculator.NONE.equals(method) ? null : life)
                .residualValue(residual)
                .depreciationMethod(method)
                .build();
        if (paidFrom != null) {
            MonetaryAssetTransaction tx = ledgerService.post(paidFrom, LedgerDirection.OUT, request.getAcquisitionValue(),
                    request.getAcquisitionDate(), LedgerCategory.ASSET_PURCHASE, REF_FIXED_ASSET, asset.getAssetId(),
                    "Asset purchase: " + asset.getName());
            asset.setPurchaseAccountCode(paidFrom);
            asset.setPurchaseTransactionId(tx.getId());
        }
        syncCurrentValue(asset);
        return FixedAssetMapper.toResponse(fixedAssetRepository.save(asset));
    }

    public PageResponse<FixedAssetResponse> getAllFixedAssets(String category, String status, Pageable pageable) {
        return PageResponse.from(fixedAssetRepository.findAll(fixedAssetSpec(category, status), pageable).map(FixedAssetMapper::toResponse));
    }

    public FixedAssetResponse getFixedAsset(String id) {
        return FixedAssetMapper.toResponse(find(id));
    }

    @Transactional
    public FixedAssetResponse updateFixedAsset(String id, FixedAssetUpdateRequest request) {
        FixedAsset asset = find(id);
        if ("disposed".equalsIgnoreCase(request.getStatus()) && asset.getDisposalDate() == null) {
            throw new BadRequestException("Use the dispose action to dispose of an asset");
        }
        boolean disposed = asset.getDisposalDate() != null;
        boolean financialChange = request.getAcquisitionDate() != null || request.getAcquisitionValue() != null;
        if (financialChange) {
            if (asset.getPurchaseTransactionId() != null) {
                throw new BadRequestException("Cost and date of an asset bought through an account cannot be edited; "
                        + "delete it (if entered in error) and re-add it");
            }
            if (disposed) {
                throw new BadRequestException("The cost and date of a disposed asset cannot be edited");
            }
        }
        if (request.getName() != null && !request.getName().isBlank()) asset.setName(request.getName());
        if (request.getCategory() != null && !request.getCategory().isBlank()) asset.setCategory(request.getCategory());
        if (request.getAcquisitionDate() != null) asset.setAcquisitionDate(request.getAcquisitionDate());
        if (request.getAcquisitionValue() != null) asset.setAcquisitionValue(request.getAcquisitionValue());
        if (request.getDepreciationMethod() != null && !request.getDepreciationMethod().isBlank()) {
            asset.setDepreciationMethod(normaliseMethod(request.getDepreciationMethod()));
        }
        if (request.getUsefulLifeYears() != null) asset.setUsefulLifeYears(request.getUsefulLifeYears());
        if (request.getResidualValue() != null) asset.setResidualValue(request.getResidualValue());
        if (DepreciationCalculator.STRAIGHT_LINE.equals(asset.getDepreciationMethod()) && asset.getUsefulLifeYears() == null) {
            throw new BadRequestException("Straight-line depreciation needs a useful life in years");
        }
        if (asset.getResidualValue().compareTo(asset.getAcquisitionValue()) > 0) {
            throw new BadRequestException("Residual value cannot exceed the acquisition value");
        }
        if (request.getLocation() != null) asset.setLocation(request.getLocation());
        if (request.getNotes() != null) asset.setNotes(request.getNotes());
        if (!disposed) {
            syncCurrentValue(asset);
        }
        return FixedAssetMapper.toResponse(fixedAssetRepository.save(asset));
    }

    /** Disposes of an asset: proceeds (if any) go into the account as ASSET_DISPOSAL; gain/loss = proceeds - NBV at disposal. */
    @Transactional
    public FixedAssetResponse disposeFixedAsset(String id, FixedAssetDisposalRequest request) {
        FixedAsset asset = find(id);
        if (asset.getDisposalDate() != null) {
            throw new BadRequestException("Asset " + asset.getName() + " is already disposed");
        }
        if (request.getDisposalDate().isBefore(asset.getAcquisitionDate())) {
            throw new BadRequestException("Disposal date cannot be before the acquisition date");
        }
        String account = blankToNull(request.getAccountCode());
        if (request.getProceeds().signum() > 0) {
            if (account == null) {
                throw new BadRequestException("Choose the account that receives the disposal proceeds");
            }
            ledgerService.requireCashOrBank(account);
            MonetaryAssetTransaction tx = ledgerService.post(account, LedgerDirection.IN, request.getProceeds(),
                    request.getDisposalDate(), LedgerCategory.ASSET_DISPOSAL, REF_FIXED_ASSET, asset.getAssetId(),
                    "Asset disposal: " + asset.getName());
            asset.setDisposalAccountCode(account);
            asset.setDisposalTransactionId(tx.getId());
        }
        asset.setDisposalDate(request.getDisposalDate());
        asset.setDisposalProceeds(request.getProceeds());
        asset.setStatus("disposed");
        asset.setCurrentValue(BigDecimal.ZERO);
        return FixedAssetMapper.toResponse(fixedAssetRepository.save(asset));
    }

    /** Deletes an asset entered in error (not disposed); reverses its purchase ledger row. */
    @Transactional
    public void deleteFixedAsset(String id) {
        FixedAsset asset = find(id);
        if (asset.getDisposalDate() != null) {
            throw new BadRequestException("A disposed asset cannot be deleted");
        }
        if (asset.getPurchaseTransactionId() != null) {
            ledgerRepository.findById(asset.getPurchaseTransactionId())
                    .ifPresent(tx -> ledgerService.reverseIfActive(tx, "Fixed asset deleted: " + asset.getName()));
        }
        fixedAssetRepository.delete(asset);
    }

    public FixedAssetSummaryResponse getFixedAssetSummary() {
        LocalDate today = LocalDate.now();
        List<FixedAsset> assets = fixedAssetRepository.findAll(fixedAssetSpec(null, "active"));

        BigDecimal totalAcquisitionValue = assets.stream().map(FixedAsset::getAcquisitionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulated = assets.stream().map(a -> DepreciationCalculator.accumulatedAt(a, today))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNbv = assets.stream().map(a -> DepreciationCalculator.nbvAt(a, today))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryTotalResponse> byCategory = assets.stream()
                .collect(Collectors.groupingBy(FixedAsset::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, a -> DepreciationCalculator.nbvAt(a, today), BigDecimal::add)))
                .entrySet().stream()
                .map(e -> CategoryTotalResponse.builder().category(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());

        return FixedAssetSummaryResponse.builder()
                .totalCount(assets.size())
                .totalAcquisitionValue(totalAcquisitionValue)
                .totalAccumulatedDepreciation(totalAccumulated)
                .totalCurrentValue(totalNbv)
                .byCategory(byCategory)
                .build();
    }

    private FixedAsset find(String id) {
        return fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FixedAsset", "id", id));
    }

    private void syncCurrentValue(FixedAsset asset) {
        asset.setCurrentValue(DepreciationCalculator.nbvAt(asset, LocalDate.now()));
    }

    private static String normaliseMethod(String method) {
        String m = method.trim().toUpperCase();
        if (!DepreciationCalculator.STRAIGHT_LINE.equals(m) && !DepreciationCalculator.NONE.equals(m)) {
            throw new BadRequestException("Depreciation method must be STRAIGHT_LINE or NONE");
        }
        return m;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
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
