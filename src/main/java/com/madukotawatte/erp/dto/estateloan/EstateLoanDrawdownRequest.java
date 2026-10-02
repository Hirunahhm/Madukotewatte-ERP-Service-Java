package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EstateLoanDrawdownRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    /** Defaults to today. */
    private LocalDate date;

    /** Optional cash/bank account receiving the money (posts ESTATE_LOAN_RECEIPT). */
    private String accountCode;
}
