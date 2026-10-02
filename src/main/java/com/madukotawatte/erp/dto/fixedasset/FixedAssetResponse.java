package com.madukotawatte.erp.dto.fixedasset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetResponse {
    private String assetId;
    private String category;
    private String name;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionValue;
    /** Computed net book value today (0 once disposed); equals netBookValue. */
    private BigDecimal currentValue;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal netBookValue;
    private String status;
    private String location;
    private String notes;
    private Integer usefulLifeYears;
    private BigDecimal residualValue;
    private String depreciationMethod;
    private String purchaseAccountCode;
    private String purchaseTransactionId;
    private LocalDate disposalDate;
    private BigDecimal disposalProceeds;
    private String disposalAccountCode;
    private String disposalTransactionId;
    /** Proceeds minus NBV at disposal (disposed assets only). */
    private BigDecimal gainLoss;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
