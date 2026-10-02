package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ManualEntryRequest {
    /** IN or OUT. */
    @NotNull
    private LedgerDirection direction;
    /** OWNER_CAPITAL, OWNER_DRAWING, OTHER_INCOME, INTEREST_INCOME, BANK_CHARGES or ADJUSTMENT. */
    @NotNull
    private LedgerCategory category;
    @NotNull
    @Positive
    private BigDecimal amount;
    /** Defaults to today. */
    private LocalDate date;
    private String description;
}
