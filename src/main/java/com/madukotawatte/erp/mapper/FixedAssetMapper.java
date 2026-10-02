package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.entity.FixedAsset;
import com.madukotawatte.erp.service.DepreciationCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FixedAssetMapper {
    private FixedAssetMapper() {}

    public static FixedAssetResponse toResponse(FixedAsset asset) {
        return toResponse(asset, LocalDate.now());
    }

    public static FixedAssetResponse toResponse(FixedAsset asset, LocalDate today) {
        boolean disposed = asset.getDisposalDate() != null;
        BigDecimal accumulated;
        BigDecimal nbv;
        BigDecimal gainLoss = null;
        if (disposed) {
            accumulated = DepreciationCalculator.accumulatedThrough(asset, asset.getDisposalDate());
            nbv = BigDecimal.ZERO.setScale(2);
            BigDecimal nbvAtDisposal = asset.getAcquisitionValue().subtract(accumulated);
            gainLoss = (asset.getDisposalProceeds() != null ? asset.getDisposalProceeds() : BigDecimal.ZERO)
                    .subtract(nbvAtDisposal);
        } else {
            accumulated = DepreciationCalculator.accumulatedAt(asset, today);
            nbv = DepreciationCalculator.nbvAt(asset, today);
        }
        return FixedAssetResponse.builder()
                .assetId(asset.getAssetId())
                .category(asset.getCategory())
                .name(asset.getName())
                .acquisitionDate(asset.getAcquisitionDate())
                .acquisitionValue(asset.getAcquisitionValue())
                .currentValue(nbv)
                .accumulatedDepreciation(accumulated)
                .netBookValue(nbv)
                .status(asset.getStatus())
                .location(asset.getLocation())
                .notes(asset.getNotes())
                .usefulLifeYears(asset.getUsefulLifeYears())
                .residualValue(asset.getResidualValue())
                .depreciationMethod(asset.getDepreciationMethod())
                .purchaseAccountCode(asset.getPurchaseAccountCode())
                .purchaseTransactionId(asset.getPurchaseTransactionId())
                .disposalDate(asset.getDisposalDate())
                .disposalProceeds(asset.getDisposalProceeds())
                .disposalAccountCode(asset.getDisposalAccountCode())
                .disposalTransactionId(asset.getDisposalTransactionId())
                .gainLoss(gainLoss)
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
