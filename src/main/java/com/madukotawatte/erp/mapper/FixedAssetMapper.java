package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.entity.FixedAsset;

public class FixedAssetMapper {
    private FixedAssetMapper() {}

    public static FixedAssetResponse toResponse(FixedAsset asset) {
        return FixedAssetResponse.builder()
                .assetId(asset.getAssetId())
                .category(asset.getCategory())
                .name(asset.getName())
                .acquisitionDate(asset.getAcquisitionDate())
                .acquisitionValue(asset.getAcquisitionValue())
                .currentValue(asset.getCurrentValue())
                .status(asset.getStatus())
                .location(asset.getLocation())
                .notes(asset.getNotes())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
