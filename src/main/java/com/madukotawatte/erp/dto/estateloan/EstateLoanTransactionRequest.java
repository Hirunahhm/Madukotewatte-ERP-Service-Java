package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstateLoanTransactionRequest {
    @NotBlank
    private String loanType;

    @NotBlank
    private String transactionType;

    @NotNull
    @Positive
    private BigDecimal amount;
}
