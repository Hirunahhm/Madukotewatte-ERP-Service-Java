package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/** Partial update of loan details (not of the money movements). */
@Data
public class EstateLoanUpdateRequest {
    private String name;
    private String lender;

    @PositiveOrZero
    private BigDecimal interestRate;

    @Positive
    private Integer termMonths;

    @PositiveOrZero
    private BigDecimal installmentAmount;

    private String notes;
}
