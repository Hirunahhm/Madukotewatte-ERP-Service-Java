package com.madukotawatte.erp.dto.fixedasset;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FixedAssetDisposalRequest {
    @NotNull
    private LocalDate disposalDate;

    @NotNull
    @PositiveOrZero
    private BigDecimal proceeds;

    /** Cash/bank account receiving the proceeds; required when proceeds is above zero. */
    private String accountCode;
}
