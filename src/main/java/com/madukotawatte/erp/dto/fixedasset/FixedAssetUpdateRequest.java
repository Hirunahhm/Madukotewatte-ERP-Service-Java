package com.madukotawatte.erp.dto.fixedasset;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedAssetUpdateRequest {
    @PositiveOrZero
    private BigDecimal currentValue;

    private String status;

    private String location;

    private String notes;
}
