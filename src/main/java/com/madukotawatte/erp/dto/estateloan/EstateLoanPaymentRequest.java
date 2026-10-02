package com.madukotawatte.erp.dto.estateloan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One repayment instalment split into principal (reduces the loan) and interest (an Interest expense). */
@Data
public class EstateLoanPaymentRequest {

    @NotNull
    @PositiveOrZero
    private BigDecimal principalAmount;

    /** Defaults to 0. */
    @PositiveOrZero
    private BigDecimal interestAmount;

    /** financial_accounts.code both parts are paid from (cash or bank). */
    @NotBlank
    private String accountCode;

    /** Defaults to today. */
    private LocalDate paymentDate;
}
