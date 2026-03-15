package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionResponse;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;

public class MonetaryAssetMapper {
    private MonetaryAssetMapper() {}

    public static MonetaryAssetTransactionResponse toResponse(MonetaryAssetTransaction transaction) {
        return MonetaryAssetTransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .assetType(transaction.getAssetType())
                .lastAmount(transaction.getLastAmount())
                .newAmount(transaction.getNewAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
