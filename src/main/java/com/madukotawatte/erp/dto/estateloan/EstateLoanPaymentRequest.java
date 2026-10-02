package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EstateLoanPaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    /** financial_accounts.code the repayment is paid from (cash or bank). */
    @NotBlank
    private String accountCode;

    /** Defaults to today. */
    private LocalDate paymentDate;
}
