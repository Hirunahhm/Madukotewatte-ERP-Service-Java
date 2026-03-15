package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EstateLoanTransactionRequest {
    @NotBlank
    private String loanType;

    @NotNull
    private BigDecimal amount;
}
