package com.madukotawatte.erp.dto.fixedasset;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Partial update. Cost and acquisition date can only change while no purchase ledger row exists. */
@Data
public class FixedAssetUpdateRequest {
    /** Deprecated and ignored: the current value is the computed net book value. */
    @PositiveOrZero
    private BigDecimal currentValue;

    /** "disposed" is rejected here; use POST /fixed-assets/{id}/dispose. */
    private String status;

    private String name;
    private String category;
    private LocalDate acquisitionDate;

    @PositiveOrZero
    private BigDecimal acquisitionValue;

    @Positive
    private Integer usefulLifeYears;

    @PositiveOrZero
    private BigDecimal residualValue;

    private String depreciationMethod;

    private String location;

    private String notes;
}
