package com.madukotawatte.erp.dto.fixedasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FixedAssetRequest {
    @NotBlank
    private String category;

    @NotBlank
    private String name;

    @NotNull
    private LocalDate acquisitionDate;

    @NotNull
    @PositiveOrZero
    private BigDecimal acquisitionValue;

    /** Deprecated and ignored: the current value is the computed net book value. */
    @PositiveOrZero
    private BigDecimal currentValue;

    /** Defaults by category (land none, building 20, vehicle 5, machinery 10, equipment/tools 5, plantation 25, other 5). */
    @Positive
    private Integer usefulLifeYears;

    /** Defaults to 0. */
    @PositiveOrZero
    private BigDecimal residualValue;

    /** STRAIGHT_LINE or NONE (default by category). */
    private String depreciationMethod;

    /** Optional account the purchase was paid from (posts ASSET_PURCHASE). Empty means a pre-existing asset. */
    private String paidFromAccountCode;

    private String location;

    private String notes;
}
