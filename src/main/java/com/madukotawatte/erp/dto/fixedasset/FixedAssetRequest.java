package com.madukotawatte.erp.dto.fixedasset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @PositiveOrZero
    private BigDecimal currentValue;

    private String location;

    private String notes;
}
