package com.madukotawatte.erp.dto.fixedasset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One asset (or, for category totals, a sum of assets) in the fixed-asset / depreciation schedule. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationRowResponse {
    private String assetId;
    private String name;
    private String category;
    private LocalDate acquisitionDate;
    private LocalDate disposalDate;
    private String depreciationMethod;
    private Integer usefulLifeYears;
    /** Original cost (sum for totals). */
    private BigDecimal cost;
    private BigDecimal costAtStart;
    /** Cost of assets acquired inside the period. */
    private BigDecimal additions;
    /** Cost of assets disposed inside the period. */
    private BigDecimal disposalCost;
    private BigDecimal costAtEnd;
    private BigDecimal accumulatedAtStart;
    private BigDecimal chargeForPeriod;
    /** Accumulated depreciation removed on disposals in the period. */
    private BigDecimal accumulatedOnDisposals;
    private BigDecimal accumulatedAtEnd;
    private BigDecimal nbvAtStart;
    private BigDecimal nbvAtEnd;
    private BigDecimal disposalProceeds;
    /** Proceeds minus NBV at disposal; zero when nothing was disposed in the period. */
    private BigDecimal disposalGainLoss;
}
