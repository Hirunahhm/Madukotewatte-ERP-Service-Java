package com.madukotawatte.erp.dto.monetary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetaryAssetTransactionResponse {
    private String id;
    private String transactionType;
    private String assetType;
    private BigDecimal lastAmount;
    private BigDecimal newAmount;
    private LocalDateTime createdAt;
}
